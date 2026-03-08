import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import * as styles from './FlowStudioPane.module.scss';
import {
  useEdgesState,
  useNodesState,
  type Connection,
  type Edge,
  type Node,
  type NodeProps,
  Handle,
  Position,
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  BackgroundVariant,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { useThemeContext } from '../../contexts/ThemeContext';
import {
  defaultEdgeData, defaultPolicySet, initialEdges, initialNodes,
  mockProviders, phaseColors, phaseLibrary, transportModesMeta,
} from './flowData';
import { searchLocations, type LocationResult } from '../../api/locationApi';
import type {
  JourneyPhase, ModeConfig, PhaseLibraryItem, PolicySet,
  RouteEdgeData, RouteNodeData, TransportMode,
} from './flowTypes';
import {
  FiChevronLeft, FiChevronRight, FiChevronDown, FiDownload, FiLayout,
  FiPlus, FiSave, FiSearch, FiX, FiTarget, FiList, FiTrash2, FiEdit2,
  FiLoader, FiCheck, FiAlertCircle,
} from 'react-icons/fi';
import { MdFlight } from 'react-icons/md';
import {
  searchPolicySets, createPolicySet, updatePolicySet, deletePolicySet,
  saveConstraints, saveNodes, saveTransitions, getConstraints, listNodes, listTransitions,
  type PolicySetDto, type PolicySetCreateRequest, type NodeRequest, type TransitionRequest,
} from './policyApi';
import { emitToast } from '../../api/client';

/* ━━━━━━━━━━━ Custom Inspector Select ━━━━━━━━━━━ */
interface InspSelectProps {
  value: string;
  options: { value: string; label: string }[];
  onChange: (value: string) => void;
}
const InspSelect: React.FC<InspSelectProps> = ({ value, options, onChange }) => {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const selected = options.find(o => o.value === value);

  useEffect(() => {
    if (!open) return;
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as HTMLElement)) setOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [open]);

  return (
    <div className={styles.inspSelect} ref={ref}>
      <button className={styles.inspSelectTrigger} onClick={() => setOpen(!open)} type="button">
        <span>{selected?.label ?? value}</span>
        <FiChevronDown size={13} className={open ? styles.inspSelectArrowOpen : ''} />
      </button>
      {open && (
        <div className={styles.inspSelectDropdown}>
          {options.map(opt => (
            <button
              key={opt.value}
              className={`${styles.inspSelectOption} ${opt.value === value ? styles.inspSelectOptionActive : ''}`}
              onClick={() => { onChange(opt.value); setOpen(false); }}
              type="button"
            >
              {opt.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};

/* ━━━━━━━━━━━ Route Phase Node ━━━━━━━━━━━ */
type RoutePhaseNodeType = Node<RouteNodeData>;
const RoutePhaseNode: React.FC<NodeProps<RoutePhaseNodeType>> = ({ data, selected }) => {
  const colors = phaseColors[data.phase as JourneyPhase];
  const isTerminal = data.phase === 'start' || data.phase === 'end';
  const enabledModes = data.modeConfigs.filter(m => m.enabled);

  return (
    <div
      className={`${styles.nodeCard} ${selected ? styles.nodeActive : ''}`}
      style={{ borderColor: colors.border, backgroundColor: colors.bg }}
    >
      {data.phase !== 'start' && <Handle type="target" position={Position.Left} className={styles.nodeHandle} />}
      {data.phase !== 'end' && <Handle type="source" position={Position.Right} className={styles.nodeHandle} />}

      <div className={styles.nodeHeader}>
        <span className={styles.phaseTag} style={{ color: colors.text, borderColor: `${colors.border}80` }}>
          {data.phase.replace(/_/g, ' ').toUpperCase()}
        </span>
      </div>
      <strong style={{ color: colors.text }}>{data.label}</strong>
      {!isTerminal && enabledModes.length > 0 && (
        <div className={styles.modeIcons}>
          {enabledModes.map(mc => (
            <span key={mc.mode} className={styles.modeIcon} style={{ color: transportModesMeta[mc.mode]?.color }} title={mc.mode}>
              {transportModesMeta[mc.mode]?.icon}
            </span>
          ))}
        </div>
      )}
      {!isTerminal && (
        <span className={styles.visitsBadge} style={{ color: colors.text }}>
          {data.minVisits}..{data.maxVisits} visits
        </span>
      )}
    </div>
  );
};

const nodeTypes = { routePhase: RoutePhaseNode };

/* ━━━━━━━━━━━ Main Component ━━━━━━━━━━━ */
export const FlowStudioPane: React.FC = () => {
  const { theme } = useThemeContext();
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);
  const [selectedNodeId, setSelectedNodeId] = useState<string>('');
  const [selectedEdgeId, setSelectedEdgeId] = useState<string>('');
  const [isInspectorCollapsed, setInspectorCollapsed] = useState(false);
  const [policySet, setPolicySet] = useState<PolicySet>({ ...defaultPolicySet });
  const [airportSearch, setAirportSearch] = useState('');
  const [showAirportDropdown, setShowAirportDropdown] = useState(false);
  const [showNodeLibrary, setShowNodeLibrary] = useState(false);
  const [inspectorWidth] = useState(400);

  // ── Airport List from DB ──
  const [airportList, setAirportList] = useState<{ iata: string; name: string; city: string; country: string }[]>([]);

  // ── Policy List State ──
  const [showPolicyList, setShowPolicyList] = useState(false);
  const [policyList, setPolicyList] = useState<PolicySetDto[]>([]);
  const [policyListLoading, setPolicyListLoading] = useState(false);
  const [policySearch, setPolicySearch] = useState('');
  const [saving, setSaving] = useState(false);
  const [activePolicyId, setActivePolicyId] = useState<string | null>(null);
  const [showNewDialog, setShowNewDialog] = useState(false);
  const [newPolicyForm, setNewPolicyForm] = useState({ code: '', scopeKey: '*', description: '' });

  // ── Fetch policy list ──
  const fetchPolicyList = useCallback(async () => {
    setPolicyListLoading(true);
    try {
      const page = await searchPolicySets({}, 0, 100);
      setPolicyList(page.content);
    } catch (e) {
      console.error('Failed to fetch policy list:', e);
    } finally {
      setPolicyListLoading(false);
    }
  }, []);

  useEffect(() => { fetchPolicyList(); }, [fetchPolicyList]);

  // ── Fetch airports from DB on mount ──
  useEffect(() => {
    (async () => {
      try {
        const locs = await searchLocations({ type: 'AIRPORT' }, 0, 200);
        setAirportList(locs.map(l => ({
          iata: l.iataCode || l.name.substring(0, 3).toUpperCase(),
          name: l.name,
          city: l.city || '',
          country: l.countryIsoCode || '',
        })));
      } catch (e) { console.error('Failed to fetch airports:', e); }
    })();
  }, []);

  // ── Load a policy from backend ──
  const loadPolicy = useCallback(async (policy: PolicySetDto) => {
    try {
      setActivePolicyId(policy.id);
      setPolicySet({
        id: policy.id,
        name: policy.code,
        code: policy.code,
        scope: policy.scopeType.value as PolicySet['scope'],
        scopeKey: policy.scopeKey,
        segment: policy.segment.value as PolicySet['segment'],
        status: policy.status.value as PolicySet['status'],
        description: policy.description,
        constraints: { ...defaultPolicySet.constraints },
        lastSaved: policy.lastModifiedDate || policy.createdDate || 'never',
      });
      // Load constraints
      try {
        const c = await getConstraints(policy.id);
        setPolicySet(prev => ({
          ...prev,
          constraints: {
            maxLegs: c.maxLegs,
            minFlights: c.minFlights,
            maxFlights: c.maxFlights,
            maxTransfers: c.maxTransfers,
            maxWalkingTotalM: c.maxWalkingTotalM ?? 2000,
            maxTotalDurationMin: c.maxTotalDurationMin ?? 720,
            minConnectionMin: c.minConnectionMinutes ?? 60,
            maxTotalCo2Grams: c.maxTotalCo2Grams,
          },
        }));
      } catch { /* constraints may not exist yet */ }
      // Load nodes → convert to React Flow nodes
      try {
        const apiNodes = await listNodes(policy.id);
        if (apiNodes.length > 0) {
          const flowNodes: Node<RouteNodeData>[] = apiNodes.map((n, i) => ({
            id: n.id || `node_${i}`,
            type: 'routePhase',
            position: { x: n.uiX ?? 60 + i * 260, y: n.uiY ?? 200 },
            data: {
              label: n.nodeKey.desc || n.nodeKey.value,
              phase: n.nodeKey.value.toLowerCase().replace(/ /g, '_') as JourneyPhase,
              modeConfigs: n.propsJson ? JSON.parse(n.propsJson).modeConfigs || [] : [],
              minVisits: n.minVisits,
              maxVisits: n.maxVisits,
            },
          }));
          setNodes(flowNodes as any);
        }
      } catch { /* nodes may not exist yet */ }
      // Load transitions → convert to React Flow edges
      try {
        const apiTransitions = await listTransitions(policy.id);
        if (apiTransitions.length > 0) {
          const flowEdges: Edge<RouteEdgeData>[] = apiTransitions.map((t, i) => ({
            id: t.id || `e_${i}`,
            source: t.fromNode.id,
            target: t.toNode.id,
            label: 'transition',
            data: {
              ...defaultEdgeData,
              priority: t.priority,
              guardJson: t.guardJson || '',
              ...(t.uiJson ? JSON.parse(t.uiJson) : {}),
            },
          }));
          setEdges(flowEdges as any);
        }
      } catch { /* transitions may not exist yet */ }
      setShowPolicyList(false);
      emitToast('success', `Policy "${policy.code}" loaded`);
    } catch (e) {
      emitToast('error', 'Failed to load policy');
    }
  }, [setNodes, setEdges]);

  // ── Save policy to backend ──
  const handleSave = useCallback(async () => {
    setSaving(true);
    try {
      let policyId = activePolicyId;
      // Create or update the policy set
      if (!policyId) {
        const reqBody: PolicySetCreateRequest = {
          code: policySet.code,
          scopeType: policySet.scope,
          scopeKey: policySet.scopeKey,
          segment: policySet.segment,
          status: 'ACTIVE',
          description: policySet.description,
        };
        const created = await createPolicySet(reqBody);
        policyId = created.id;
        setActivePolicyId(policyId);
      } else {
        const reqBody: PolicySetCreateRequest = {
          code: policySet.code,
          scopeType: policySet.scope,
          scopeKey: policySet.scopeKey,
          segment: policySet.segment,
          status: 'ACTIVE',
          description: policySet.description,
        };
        await updatePolicySet(policyId, reqBody);
      }
      // Save constraints
      await saveConstraints(policyId, {
        maxLegs: policySet.constraints.maxLegs,
        minFlights: policySet.constraints.minFlights,
        maxFlights: policySet.constraints.maxFlights,
        minTransfers: 0,
        maxTransfers: policySet.constraints.maxTransfers,
        maxTotalDurationMin: policySet.constraints.maxTotalDurationMin,
        maxWalkingTotalM: policySet.constraints.maxWalkingTotalM,
        minConnectionMinutes: policySet.constraints.minConnectionMin,
        maxTotalCo2Grams: policySet.constraints.maxTotalCo2Grams,
      });
      // Save nodes
      const nodePayload: NodeRequest[] = nodes.map(n => ({
        nodeKey: (n.data.phase || 'START').toUpperCase(),
        minVisits: n.data.minVisits,
        maxVisits: n.data.maxVisits,
        propsJson: JSON.stringify({ modeConfigs: n.data.modeConfigs, maxLegsInPhase: n.data.maxLegsInPhase, notes: n.data.notes }),
        uiX: Math.round(n.position.x),
        uiY: Math.round(n.position.y),
      }));
      const savedNodes = await saveNodes(policyId, nodePayload);
      // Build node ID map for transitions
      const nodeIdMap: Record<string, string> = {};
      nodes.forEach((n, i) => { if (savedNodes[i]?.id) nodeIdMap[n.id] = savedNodes[i].id!; });
      // Save transitions
      const transPayload: TransitionRequest[] = edges.map(e => ({
        fromNodeId: nodeIdMap[e.source] || e.source,
        toNodeId: nodeIdMap[e.target] || e.target,
        priority: (e.data as RouteEdgeData)?.priority ?? 1,
        guardJson: (e.data as RouteEdgeData)?.guardJson || undefined,
        uiJson: JSON.stringify({
          sameAirport: (e.data as RouteEdgeData)?.sameAirport,
          requireSameTerminal: (e.data as RouteEdgeData)?.requireSameTerminal,
          maxDurationMin: (e.data as RouteEdgeData)?.maxDurationMin,
          maxWalkingM: (e.data as RouteEdgeData)?.maxWalkingM,
          minConnectionMin: (e.data as RouteEdgeData)?.minConnectionMin,
        }),
      }));
      await saveTransitions(policyId, transPayload);
      setPolicySet(prev => ({ ...prev, status: 'ACTIVE', lastSaved: new Date().toLocaleTimeString() }));
      emitToast('success', `Policy "${policySet.code}" saved successfully`);
      fetchPolicyList();
    } catch (e: any) {
      emitToast('error', e.message || 'Failed to save policy');
    } finally {
      setSaving(false);
    }
  }, [activePolicyId, policySet, nodes, edges, fetchPolicyList]);

  // ── Create new policy ──
  const handleCreatePolicy = useCallback(async () => {
    if (!newPolicyForm.code.trim()) {
      emitToast('error', 'Policy code is required');
      return;
    }
    try {
      const created = await createPolicySet({
        code: newPolicyForm.code.trim().toUpperCase(),
        scopeType: 'GLOBAL',
        scopeKey: newPolicyForm.scopeKey || '*',
        segment: 'DEFAULT',
        status: 'DRAFT',
        description: newPolicyForm.description,
      });
      setShowNewDialog(false);
      setNewPolicyForm({ code: '', scopeKey: '*', description: '' });
      await fetchPolicyList();
      await loadPolicy(created);
      emitToast('success', `Policy "${created.code}" created`);
    } catch (e: any) {
      emitToast('error', e.message || 'Failed to create policy');
    }
  }, [newPolicyForm, fetchPolicyList, loadPolicy]);

  // ── Delete a policy ──
  const handleDeletePolicy = useCallback(async (id: string) => {
    try {
      await deletePolicySet(id);
      if (activePolicyId === id) {
        setActivePolicyId(null);
        setPolicySet({ ...defaultPolicySet });
        setNodes(initialNodes as any);
        setEdges(initialEdges as any);
      }
      await fetchPolicyList();
      emitToast('success', 'Policy deleted');
    } catch (e: any) {
      emitToast('error', e.message || 'Failed to delete policy');
    }
  }, [activePolicyId, fetchPolicyList, setNodes, setEdges]);

  const selectedNode = useMemo(() => nodes.find(n => n.id === selectedNodeId) ?? null, [nodes, selectedNodeId]);
  const selectedEdge = useMemo(() => edges.find(e => e.id === selectedEdgeId) ?? null, [edges, selectedEdgeId]);

  const filteredAirports = useMemo(() => {
    const q = airportSearch.toLowerCase();
    if (!q) return airportList;
    return airportList.filter(a =>
      a.iata.toLowerCase().includes(q) || a.name.toLowerCase().includes(q) || a.city.toLowerCase().includes(q)
    );
  }, [airportSearch, airportList]);

  const edgeColor = theme.variant === 'dark' ? 'rgba(200, 16, 46, 0.55)' : 'rgba(200, 16, 46, 0.4)';
  const bgGridColor = theme.variant === 'dark' ? 'rgba(148, 163, 184, 0.12)' : 'rgba(200, 16, 46, 0.12)';
  const panelRight = isInspectorCollapsed ? 64 : inspectorWidth;

  // Track which phases already exist — each phase can only appear once
  const existingPhases = useMemo(() => {
    const set = new Set<string>();
    nodes.forEach(n => { if (n.data?.phase) set.add(n.data.phase); });
    return set;
  }, [nodes]);

  /* ── Handlers ── */
  const onConnect = useCallback((c: Connection) => {
    const newEdge: Edge<RouteEdgeData> = {
      id: `e_${c.source}_${c.target}_${Date.now()}`,
      source: c.source!,
      target: c.target!,
      label: 'transition',
      data: { ...defaultEdgeData },
    };
    setEdges(eds => [...eds, newEdge]);
  }, [setEdges]);

  const handleAddNode = (item: PhaseLibraryItem) => {
    // Block duplicate phases
    if (existingPhases.has(item.phase)) return;

    const id = `node_${item.phase}_${Date.now()}`;
    const newNode: Node<RouteNodeData> = {
      id,
      type: 'routePhase',
      position: { x: 200 + nodes.length * 80, y: 200 + (nodes.length % 3) * 120 },
      data: {
        label: item.label,
        phase: item.phase,
        modeConfigs: item.defaultModes.map(m => ({ ...m })),
        minVisits: item.phase === 'start' || item.phase === 'end' ? 1 : 0,
        maxVisits: item.phase === 'main_haul' ? 2 : 1,
      },
    };
    setNodes((prev: any) => [...prev, newNode]);
    setSelectedNodeId(id);
    setShowNodeLibrary(false);
  };

  const updateNodeData = (patch: Partial<RouteNodeData>) => {
    if (!selectedNodeId) return;
    setNodes(prev =>
      prev.map(n => n.id === selectedNodeId ? { ...n, data: { ...n.data, ...patch } } : n)
    );
  };

  const toggleMode = (mode: TransportMode) => {
    if (!selectedNode) return;
    const updated = selectedNode.data.modeConfigs.map(mc =>
      mc.mode === mode ? { ...mc, enabled: !mc.enabled } : mc
    );
    updateNodeData({ modeConfigs: updated });
  };

  const updateModeConfig = (mode: TransportMode, patch: Partial<ModeConfig>) => {
    if (!selectedNode) return;
    const updated = selectedNode.data.modeConfigs.map(mc =>
      mc.mode === mode ? { ...mc, ...patch } : mc
    );
    updateNodeData({ modeConfigs: updated });
  };

  const updateEdgeData = (patch: Partial<RouteEdgeData>) => {
    if (!selectedEdgeId) return;
    setEdges(prev =>
      prev.map(e => e.id === selectedEdgeId
        ? { ...e, data: { ...(e.data as RouteEdgeData ?? defaultEdgeData), ...patch } }
        : e
      )
    );
  };

  const handleAutoLayout = useCallback(() => {
    const spacingX = 270;
    const startX = 80;
    const y = 220;
    setNodes(prev => prev.map((n, i) => ({ ...n, position: { x: startX + i * spacingX, y } })));
  }, [setNodes]);

  const handleExport = () => {
    const payload = { policy: policySet, nodes, edges, exportedAt: new Date().toISOString() };
    const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `policy-${policySet.scopeKey || 'global'}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const hubAirport = airportList.find(a => a.iata === policySet.scopeKey);

  /* ━━━━━━━━━━━ RENDER ━━━━━━━━━━━ */
  return (
    <div className={styles.studioWrapper}>
      {/* ── TOP BAR ── */}
      <div className={styles.topBar}>
        <div className={styles.topBarLeft}>
          <MdFlight size={20} style={{ color: '#C8102E' }} />
          <input
            className={styles.policyNameInput}
            value={policySet.name}
            onChange={e => setPolicySet(p => ({ ...p, name: e.target.value }))}
          />
          <span className={`${styles.statusBadge} ${styles[policySet.status.toLowerCase()]}`}>
            {policySet.status}
          </span>
        </div>
        <div className={styles.topBarCenter}>
          <div className={styles.airportSelector}>
            <button
              className={styles.airportButton}
              onClick={() => setShowAirportDropdown(!showAirportDropdown)}
            >
              <FiTarget size={14} />
              <span>{hubAirport ? `${hubAirport.iata} — ${hubAirport.name}` : policySet.scopeKey === '*' ? 'Global (All)' : policySet.scopeKey}</span>
            </button>
            {showAirportDropdown && (
              <div className={styles.airportDropdown}>
                <div className={styles.airportSearchWrap}>
                  <FiSearch size={14} />
                  <input
                    placeholder="Search airports..."
                    value={airportSearch}
                    onChange={e => setAirportSearch(e.target.value)}
                    autoFocus
                  />
                </div>
                <div className={styles.airportList}>
                  <button
                    className={`${styles.airportItem} ${policySet.scopeKey === '*' ? styles.airportActive : ''}`}
                    onClick={() => { setPolicySet(p => ({ ...p, scopeKey: '*', scope: 'GLOBAL' })); setShowAirportDropdown(false); }}
                  >
                    <strong>*</strong><span>Global (All Airports)</span><span className={styles.airportCity}>Worldwide</span>
                  </button>
                  {filteredAirports.map(a => (
                    <button
                      key={a.iata}
                      className={`${styles.airportItem} ${a.iata === policySet.scopeKey ? styles.airportActive : ''}`}
                      onClick={() => {
                        setPolicySet(p => ({ ...p, scopeKey: a.iata, scope: 'AIRPORT' }));
                        setShowAirportDropdown(false);
                        setAirportSearch('');
                      }}
                    >
                      <strong>{a.iata}</strong>
                      <span>{a.name}</span>
                      <span className={styles.airportCity}>{a.city}, {a.country}</span>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
        <div className={styles.topBarRight}>
          <button className={styles.toolBtn} onClick={() => setShowPolicyList(!showPolicyList)} title="Policy List"><FiList /></button>
          <button className={styles.toolBtn} onClick={handleAutoLayout} title="Auto Layout"><FiLayout /></button>
          <button className={styles.toolBtn} onClick={handleExport} title="Export JSON"><FiDownload /></button>
          <button className={`${styles.toolBtn} ${styles.primary}`} onClick={handleSave} disabled={saving} title="Save">
            {saving ? <FiLoader className={styles.spinIcon} /> : <FiSave />}
          </button>
        </div>
      </div>

      {/* ── CANVAS + INSPECTOR ── */}
      <div className={styles.canvasRow}>
        {/* Add Node FAB */}
        <div className={styles.addNodeFab}>
          <button className={styles.fabButton} onClick={() => setShowNodeLibrary(!showNodeLibrary)}>
            {showNodeLibrary ? <FiX /> : <FiPlus />}
          </button>
          {showNodeLibrary && (
            <div className={styles.nodeLibrary}>
              <h4>Add Phase</h4>
              {phaseLibrary.map(item => {
                const alreadyExists = existingPhases.has(item.phase);
                return (
                  <button
                    key={item.id}
                    className={`${styles.libraryItem} ${alreadyExists ? styles.libraryItemDisabled : ''}`}
                    onClick={() => !alreadyExists && handleAddNode(item)}
                    disabled={alreadyExists}
                    title={alreadyExists ? `${item.label} already in graph` : `Add ${item.label}`}
                  >
                    <span className={styles.libraryIcon} style={{ color: alreadyExists ? undefined : phaseColors[item.phase].border }}>
                      {item.icon}
                    </span>
                    <div>
                      <strong>{item.label}</strong>
                      <span>{alreadyExists ? 'Already in graph' : item.description}</span>
                    </div>
                  </button>
                );
              })}
            </div>
          )}
        </div>

        {/* React Flow Canvas */}
        <div className={styles.canvasArea} style={{ marginRight: panelRight }}>
          <ReactFlow
            nodes={nodes}
            edges={edges.map(e => ({ ...e, style: { ...e.style, stroke: edgeColor, strokeWidth: 2 } }))}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onNodeClick={(_, node) => { setSelectedNodeId(node.id); setSelectedEdgeId(''); }}
            onEdgeClick={(_, edge) => { setSelectedEdgeId(edge.id); setSelectedNodeId(''); }}
            onPaneClick={() => { setSelectedNodeId(''); setSelectedEdgeId(''); }}
            nodeTypes={nodeTypes}
            fitView
            proOptions={{ hideAttribution: true }}
            colorMode={theme.variant}
          >
            <Background variant={BackgroundVariant.Dots} gap={20} size={1} color={bgGridColor} />
            <Controls />
            <MiniMap
              nodeColor={n => phaseColors[(n.data as RouteNodeData)?.phase]?.border ?? '#6b7280'}
              maskColor={theme.variant === 'dark' ? 'rgba(0,0,0,0.7)' : 'rgba(255,255,255,0.7)'}
            />
          </ReactFlow>
        </div>

        {/* ── INSPECTOR ── */}
        <div className={`${styles.inspector} ${isInspectorCollapsed ? styles.inspectorCollapsed : ''}`}
             style={{ width: isInspectorCollapsed ? 56 : inspectorWidth }}>

          {isInspectorCollapsed ? (
            <div className={styles.collapsedPanel} onClick={() => setInspectorCollapsed(false)}>
              <span className={styles.collapsedLabel}>
                {selectedNode ? 'Phase Rules' : selectedEdge ? 'Transition Guards' : 'Policy Constraints'}
              </span>
              <button className={styles.collapseBtn}><FiChevronLeft /></button>
            </div>
          ) : (
            <div className={styles.inspectorContent}>
              {/* ═══ Node Selected — Phase Rules ═══ */}
              {selectedNode && (
                <>
                  <div className={styles.inspectorHeader}>
                    <h3 className={styles.inspectorTitle}>Phase Rules</h3>
                    <button className={styles.collapseBtn} onClick={() => setInspectorCollapsed(true)}><FiChevronRight /></button>
                  </div>

                  {/* Phase Type Badge */}
                  <div className={styles.inspectorSection}>
                    <label>Phase</label>
                    <span className={styles.phaseTagInsp} style={{
                      color: phaseColors[selectedNode.data.phase].text,
                      borderColor: phaseColors[selectedNode.data.phase].border,
                      backgroundColor: phaseColors[selectedNode.data.phase].bg,
                    }}>
                      {selectedNode.data.phase.replace(/_/g, ' ').toUpperCase()}
                    </span>
                  </div>

                  {/* Label */}
                  <div className={styles.inspectorSection}>
                    <label>Label</label>
                    <input
                      className={styles.inspInput}
                      value={selectedNode.data.label}
                      onChange={e => updateNodeData({ label: e.target.value })}
                    />
                  </div>

                  {/* Mode Configs — Uber-style per-mode cards */}
                  {selectedNode.data.phase !== 'start' && selectedNode.data.phase !== 'end' && (
                    <>
                      <div className={styles.inspectorSection}>
                        <label>Allowed Transport Modes</label>
                        <div className={styles.modeCardList}>
                          {selectedNode.data.modeConfigs.map(mc => {
                            const meta = transportModesMeta[mc.mode];
                            const providers = mockProviders.filter(p => p.applicableModes.includes(mc.mode));
                            return (
                              <div key={mc.mode} className={`${styles.modeCard} ${mc.enabled ? styles.modeCardEnabled : ''}`}>
                                <div className={styles.modeCardHeader}>
                                  <label className={styles.modeToggle}>
                                    <input type="checkbox" checked={mc.enabled} onChange={() => toggleMode(mc.mode)} />
                                    <span className={styles.modeToggleIcon} style={{ color: mc.enabled ? meta.color : undefined }}>
                                      {meta.icon}
                                    </span>
                                    <span className={styles.modeToggleLabel}>{meta.label}</span>
                                  </label>
                                </div>
                                {mc.enabled && (
                                  <div className={styles.modeCardBody}>
                                    {providers.length > 0 && (
                                      <div className={styles.modeField}>
                                        <span>Provider</span>
                                        <InspSelect
                                          value={mc.providerId ?? ''}
                                          options={[
                                            { value: '', label: 'Any' },
                                            ...providers.map(p => ({ value: p.id, label: p.name })),
                                          ]}
                                          onChange={v => {
                                            const prov = mockProviders.find(p => p.id === v);
                                            updateModeConfig(mc.mode, { providerId: v || undefined, providerName: prov?.name });
                                          }}
                                        />
                                      </div>
                                    )}
                                    {mc.mode !== 'FLIGHT' && (
                                      <div className={styles.modeField}>
                                        <span>Max Walk Access</span>
                                        <input type="number" className={styles.inspInput}
                                          value={mc.maxWalkingAccessM ?? ''}
                                          onChange={e => updateModeConfig(mc.mode, { maxWalkingAccessM: e.target.value ? Number(e.target.value) : undefined })}
                                          placeholder="No limit (m)"
                                        />
                                      </div>
                                    )}
                                    <div className={styles.modeField}>
                                      <span>Max Duration</span>
                                      <input type="number" className={styles.inspInput}
                                        value={mc.maxDurationMin ?? ''}
                                        onChange={e => updateModeConfig(mc.mode, { maxDurationMin: e.target.value ? Number(e.target.value) : undefined })}
                                        placeholder="No limit (min)"
                                      />
                                    </div>
                                  </div>
                                )}
                              </div>
                            );
                          })}
                        </div>
                      </div>

                      {/* Visits + Max Legs */}
                      <div className={styles.inspectorSection}>
                        <label>Visits</label>
                        <div className={styles.visitsRow}>
                          <div>
                            <span>Min</span>
                            <input type="number" min={0} max={10}
                              className={styles.inspInput}
                              value={selectedNode.data.minVisits}
                              onChange={e => updateNodeData({ minVisits: Number(e.target.value) })}
                            />
                          </div>
                          <div>
                            <span>Max</span>
                            <input type="number" min={0} max={10}
                              className={styles.inspInput}
                              value={selectedNode.data.maxVisits}
                              onChange={e => updateNodeData({ maxVisits: Number(e.target.value) })}
                            />
                          </div>
                        </div>
                      </div>
                      {selectedNode.data.maxLegsInPhase !== undefined && (
                        <div className={styles.inspectorSection}>
                          <label>Max Legs in Phase</label>
                          <input type="number" className={styles.inspInput} min={1} max={5}
                            value={selectedNode.data.maxLegsInPhase ?? ''}
                            onChange={e => updateNodeData({ maxLegsInPhase: e.target.value ? Number(e.target.value) : undefined })}
                          />
                        </div>
                      )}
                    </>
                  )}

                  <div className={styles.inspectorSection}>
                    <label>Notes</label>
                    <textarea
                      className={styles.inspTextarea}
                      value={selectedNode.data.notes ?? ''}
                      onChange={e => updateNodeData({ notes: e.target.value })}
                      placeholder="Optional notes..."
                      rows={2}
                    />
                  </div>
                </>
              )}

              {/* ═══ Edge Selected ═══ */}
              {selectedEdge && !selectedNode && (
                <>
                  <div className={styles.inspectorHeader}>
                    <h3 className={styles.inspectorTitle}>Transition Guards</h3>
                    <button className={styles.collapseBtn} onClick={() => setInspectorCollapsed(true)}><FiChevronRight /></button>
                  </div>
                  <div className={styles.inspectorSection}>
                    <label>From → To</label>
                    <span className={styles.edgeEndpoints}>
                      {nodes.find(n => n.id === selectedEdge.source)?.data.label ?? '?'}
                      {' → '}
                      {nodes.find(n => n.id === selectedEdge.target)?.data.label ?? '?'}
                    </span>
                  </div>
                  <div className={styles.inspectorSection}>
                    <label className={styles.toggleRow}>
                      <input type="checkbox"
                        checked={(selectedEdge.data as RouteEdgeData)?.sameAirport ?? false}
                        onChange={e => updateEdgeData({ sameAirport: e.target.checked })}
                      />
                      Same Airport Required
                    </label>
                  </div>
                  <div className={styles.inspectorSection}>
                    <label className={styles.toggleRow}>
                      <input type="checkbox"
                        checked={(selectedEdge.data as RouteEdgeData)?.requireSameTerminal ?? false}
                        onChange={e => updateEdgeData({ requireSameTerminal: e.target.checked })}
                      />
                      Same Terminal Required
                    </label>
                  </div>
                  <div className={styles.inspectorSection}>
                    <label>Min Connection Time (min)</label>
                    <input type="number" className={styles.inspInput}
                      value={(selectedEdge.data as RouteEdgeData)?.minConnectionMin ?? ''}
                      onChange={e => updateEdgeData({ minConnectionMin: e.target.value ? Number(e.target.value) : undefined })}
                      placeholder="No minimum"
                    />
                  </div>
                  <div className={styles.inspectorSection}>
                    <label>Max Duration (min)</label>
                    <input type="number" className={styles.inspInput}
                      value={(selectedEdge.data as RouteEdgeData)?.maxDurationMin ?? ''}
                      onChange={e => updateEdgeData({ maxDurationMin: e.target.value ? Number(e.target.value) : undefined })}
                      placeholder="No limit"
                    />
                  </div>
                  <div className={styles.inspectorSection}>
                    <label>Max Walking (m)</label>
                    <input type="number" className={styles.inspInput}
                      value={(selectedEdge.data as RouteEdgeData)?.maxWalkingM ?? ''}
                      onChange={e => updateEdgeData({ maxWalkingM: e.target.value ? Number(e.target.value) : undefined })}
                      placeholder="No limit"
                    />
                  </div>
                  <div className={styles.inspectorSection}>
                    <label>Priority</label>
                    <input type="number" className={styles.inspInput} min={1} max={10}
                      value={(selectedEdge.data as RouteEdgeData)?.priority ?? 1}
                      onChange={e => updateEdgeData({ priority: Number(e.target.value) })}
                    />
                  </div>
                  <div className={styles.inspectorSection}>
                    <label>Guard (JSON)</label>
                    <textarea
                      className={styles.inspTextarea}
                      value={(selectedEdge.data as RouteEdgeData)?.guardJson ?? ''}
                      onChange={e => updateEdgeData({ guardJson: e.target.value })}
                      placeholder='{"key": "value"}'
                      rows={3}
                    />
                  </div>
                </>
              )}

              {/* ═══ Nothing selected — Policy Constraints ═══ */}
              {!selectedNode && !selectedEdge && (
                <>
                  <div className={styles.inspectorHeader}>
                    <h3 className={styles.inspectorTitle}>Policy Constraints</h3>
                    <button className={styles.collapseBtn} onClick={() => setInspectorCollapsed(true)}><FiChevronRight /></button>
                  </div>
                  <div className={styles.inspectorSection}>
                    <label>Scope</label>
                    <InspSelect
                      value={policySet.scope}
                      options={[
                        { value: 'GLOBAL', label: 'Global' },
                        { value: 'COUNTRY', label: 'Country' },
                        { value: 'REGION', label: 'Region' },
                        { value: 'AIRPORT', label: 'Airport' },
                        { value: 'AIRPORT_PAIR', label: 'Airport Pair' },
                      ]}
                      onChange={v => setPolicySet(p => ({ ...p, scope: v as PolicySet['scope'] }))}
                    />
                  </div>
                  <div className={styles.inspectorSection}>
                    <label>Segment</label>
                    <InspSelect
                      value={policySet.segment}
                      options={[
                        { value: 'DEFAULT', label: 'Default' },
                        { value: 'CORPORATE', label: 'Corporate' },
                        { value: 'ELITE', label: 'Elite' },
                        { value: 'IRROPS', label: 'Irregular Ops' },
                      ]}
                      onChange={v => setPolicySet(p => ({ ...p, segment: v as PolicySet['segment'] }))}
                    />
                  </div>
                  <div className={styles.inspectorSection}>
                    <label>Max Legs</label>
                    <input type="number" className={styles.inspInput}
                      value={policySet.constraints.maxLegs}
                      onChange={e => setPolicySet(p => ({ ...p, constraints: { ...p.constraints, maxLegs: Number(e.target.value) } }))}
                    />
                  </div>
                  <div className={styles.inspectorSection}>
                    <label>Flights (Min / Max)</label>
                    <div className={styles.visitsRow}>
                      <div>
                        <span>Min</span>
                        <input type="number" className={styles.inspInput}
                          value={policySet.constraints.minFlights}
                          onChange={e => setPolicySet(p => ({ ...p, constraints: { ...p.constraints, minFlights: Number(e.target.value) } }))}
                        />
                      </div>
                      <div>
                        <span>Max</span>
                        <input type="number" className={styles.inspInput}
                          value={policySet.constraints.maxFlights}
                          onChange={e => setPolicySet(p => ({ ...p, constraints: { ...p.constraints, maxFlights: Number(e.target.value) } }))}
                        />
                      </div>
                    </div>
                  </div>
                  <div className={styles.inspectorSection}>
                    <label>Max Transfers</label>
                    <input type="number" className={styles.inspInput}
                      value={policySet.constraints.maxTransfers}
                      onChange={e => setPolicySet(p => ({ ...p, constraints: { ...p.constraints, maxTransfers: Number(e.target.value) } }))}
                    />
                  </div>
                  <div className={styles.inspectorSection}>
                    <label>Min Connection (min)</label>
                    <input type="number" className={styles.inspInput}
                      value={policySet.constraints.minConnectionMin}
                      onChange={e => setPolicySet(p => ({ ...p, constraints: { ...p.constraints, minConnectionMin: Number(e.target.value) } }))}
                    />
                  </div>
                  <div className={styles.inspectorSection}>
                    <label>Max Walking Total (m)</label>
                    <input type="number" className={styles.inspInput}
                      value={policySet.constraints.maxWalkingTotalM}
                      onChange={e => setPolicySet(p => ({ ...p, constraints: { ...p.constraints, maxWalkingTotalM: Number(e.target.value) } }))}
                    />
                  </div>
                  <div className={styles.inspectorSection}>
                    <label>Max Total Duration (min)</label>
                    <input type="number" className={styles.inspInput}
                      value={policySet.constraints.maxTotalDurationMin}
                      onChange={e => setPolicySet(p => ({ ...p, constraints: { ...p.constraints, maxTotalDurationMin: Number(e.target.value) } }))}
                    />
                  </div>
                  <div className={styles.inspectorSection}>
                    <label>Status</label>
                    <InspSelect
                      value={policySet.status}
                      options={[
                        { value: 'DRAFT', label: 'Draft' },
                        { value: 'ACTIVE', label: 'Active' },
                        { value: 'DEPRECATED', label: 'Deprecated' },
                      ]}
                      onChange={v => setPolicySet(p => ({ ...p, status: v as PolicySet['status'] }))}
                    />
                  </div>
                  <div className={styles.inspectorSection}>
                    <label>Description</label>
                    <textarea
                      className={styles.inspTextarea}
                      value={policySet.description ?? ''}
                      onChange={e => setPolicySet(p => ({ ...p, description: e.target.value }))}
                      placeholder="Policy description..."
                      rows={2}
                    />
                  </div>
                </>
              )}
            </div>
          )}
        </div>
      </div>

      {/* ── POLICY LIST DRAWER ── */}
      {showPolicyList && (
        <div className={styles.policyListOverlay} onClick={() => setShowPolicyList(false)}>
          <div className={styles.policyListDrawer} onClick={e => e.stopPropagation()}>
            <div className={styles.policyListHeader}>
              <h3>Policy Sets</h3>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button className={styles.toolBtn} onClick={() => setShowNewDialog(true)} title="New Policy">
                  <FiPlus />
                </button>
                <button className={styles.toolBtn} onClick={() => setShowPolicyList(false)}>
                  <FiX />
                </button>
              </div>
            </div>

            <div className={styles.policyListSearch}>
              <FiSearch size={14} />
              <input placeholder="Search policies..." value={policySearch} onChange={e => setPolicySearch(e.target.value)} />
            </div>

            {policyListLoading ? (
              <div style={{ textAlign: 'center', padding: '2rem', opacity: 0.5 }}>
                <FiLoader className={styles.spinIcon} size={20} />
              </div>
            ) : (
              <div className={styles.policyListItems}>
                {policyList
                  .filter(p => !policySearch || p.code.toLowerCase().includes(policySearch.toLowerCase()) ||
                    p.scopeKey.toLowerCase().includes(policySearch.toLowerCase()))
                  .map(p => (
                    <div key={p.id}
                      className={`${styles.policyListItem} ${p.id === activePolicyId ? styles.policyListItemActive : ''}`}
                      onClick={() => loadPolicy(p)}
                    >
                      <div className={styles.policyListItemInfo}>
                        <strong>{p.code}</strong>
                        <span className={styles.policyListScope}>
                          {p.scopeType.value} · {p.scopeKey}
                        </span>
                        {p.description && <span className={styles.policyListDesc}>{p.description}</span>}
                      </div>
                      <div className={styles.policyListItemActions}>
                        <span className={`${styles.statusBadge} ${styles[p.status.value.toLowerCase()]}`}>
                          {p.status.value}
                        </span>
                        <button className={styles.policyDeleteBtn}
                          onClick={e => { e.stopPropagation(); handleDeletePolicy(p.id); }}
                          title="Delete"
                        >
                          <FiTrash2 size={13} />
                        </button>
                      </div>
                    </div>
                  ))}
                {policyList.length === 0 && (
                  <div style={{ textAlign: 'center', padding: '2rem', opacity: 0.4 }}>
                    No policies found. Create one to get started.
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {/* ── NEW POLICY DIALOG ── */}
      {showNewDialog && (
        <div className={styles.policyListOverlay} onClick={() => setShowNewDialog(false)}>
          <div className={styles.newPolicyDialog} onClick={e => e.stopPropagation()}>
            <div className={styles.policyListHeader}>
              <h3>New Policy Set</h3>
              <button className={styles.toolBtn} onClick={() => setShowNewDialog(false)}><FiX /></button>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', padding: '0 1rem 1rem' }}>
              <div>
                <label className={styles.newPolicyLabel}>Code *</label>
                <input className={styles.inspInput} placeholder="e.g. SAW_DEFAULT"
                  value={newPolicyForm.code}
                  onChange={e => setNewPolicyForm(f => ({ ...f, code: e.target.value }))}
                />
              </div>
              <div>
                <label className={styles.newPolicyLabel}>Scope Key</label>
                <input className={styles.inspInput} placeholder="* (global) or IATA code"
                  value={newPolicyForm.scopeKey}
                  onChange={e => setNewPolicyForm(f => ({ ...f, scopeKey: e.target.value }))}
                />
              </div>
              <div>
                <label className={styles.newPolicyLabel}>Description</label>
                <textarea className={styles.inspTextarea} rows={2}
                  placeholder="Optional description..."
                  value={newPolicyForm.description}
                  onChange={e => setNewPolicyForm(f => ({ ...f, description: e.target.value }))}
                />
              </div>
              <button className={`${styles.toolBtn} ${styles.primary}`}
                style={{ alignSelf: 'flex-end', padding: '0.5rem 1.25rem', fontSize: '0.85rem' }}
                onClick={handleCreatePolicy}
              >
                <FiPlus size={14} /> Create Policy
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
