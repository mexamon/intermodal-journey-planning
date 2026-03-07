// File: src/app/types/index.ts

/**
 * Çalışma alanının (Workspace) yapısını tanımlar.
 */
export interface Workspace {
    id: string;
    name: string;
    ownerEmail: string;
    plan: string;
    logoUrl: string;
    billingEmail: string;
    description: string;
}

export interface Team {
  id: string;
  name: string;
  color: string; // Etiketler için renk kodu
}

/**
 * Bir kullanıcının yetkilerini tanımlar.
 */
export interface UserPermissions {
    createRooms: boolean;
    publishTemplates: boolean;
    seeOpenRooms: boolean;
    isAdmin: boolean;
}

/**
 * Çalışma alanındaki bir kullanıcının yapısını tanımlar.
 */
export interface User {
    id: string;
    name: string;
    email: string;
    avatarUrl: string;
    role: 'Workspace admin' | 'Member' | 'Guest';
    lastAccess: string;
    status: 'Active' | 'Inactive';
    joinDate: string;
    permissions: UserPermissions;
    teams: string[]; // <-- HATA ALMAMAK İÇİN BU SATIRI EKLEYİN
}

/**
 * Beklemedeki bir davetiyenin yapısını tanımlar.
 */
export interface PendingInvite {
    id: string;
    email: string;
    invitedBy: string;
    date: string;
    role: 'Member' | 'Guest';
}

/**
 * Fiyatlandırma planının yapısını tanımlar.
 */
export interface Plan {
    name: 'Free' | 'Pro' | 'Enterprise';
    price: number;
    description: string;
    features: string[];
    cta: string;
    isCurrent?: boolean;
}

/**
 * Bir API anahtarının yapısını tanımlar.
 */
export interface ApiKey {
    id: string;
    name: string;
    token: string;
    createdAt: string;
    lastUsed: string | null;
}

/**
 * Denetim kaydının (Audit Log) yapısını tanımlar.
 */
export interface AuditLog {
    id: number;
    user: {
        name: string;
        avatarUrl: string;
    };
    action: string;
    date: string;
    ip: string;
}


export interface Invoice {
    id: string;
    date: string;
    amount: number;
    status: 'Paid' | 'Pending' | 'Failed';
}

export type ProjectEnvironment = 'Sandbox' | 'Staging' | 'Production';
export type ProjectStatus = 'Active' | 'Draft' | 'Paused';

export interface Project {
    id: string;
    name: string;
    domain: string;
    description: string;
    environment: ProjectEnvironment;
    status: ProjectStatus;
    owner: string;
    lastRun: string;
    agentCount: number;
    flowCount: number;
    tags: string[];
}

export type AgentStatus = 'Active' | 'Draft' | 'Paused';

export interface DomainAgent {
    id: string;
    name: string;
    domain: string;
    summary: string;
    status: AgentStatus;
    modelRoute: string;
    policy: string;
    tools: string[];
    updatedAt: string;
    owner: string;
    projectId?: string;
    projectName?: string;
}

export type AgentProfileStatus = 'Active' | 'Draft' | 'Paused';
export type AgentMemoryMode = 'stateless' | 'session' | 'long-term';

export interface AgentProfile {
    id: string;
    name: string;
    role: string;
    modelProfileId: string;
    policyProfile: string;
    toolAccess: string[];
    memory: AgentMemoryMode;
    status: AgentProfileStatus;
    updatedAt: string;
}

export interface DomainSpec {
    id: string;
    name: string;
    description: string;
    riskTier: 'Low' | 'Medium' | 'High';
}

export type FlowNodeType = 'trigger' | 'agent' | 'tool' | 'policy' | 'router' | 'compose' | 'knowledge';

export interface FlowNode {
    id: string;
    type: FlowNodeType;
    label: string;
    x: number;
    y: number;
    summary?: string;
    config?: Record<string, unknown>;
}

export interface FlowEdge {
    id: string;
    from: string;
    to: string;
    label?: string;
}

export type ToolType = 'adapter' | 'connector' | 'action' | 'knowledge' | 'approval';
export type ToolStatus = 'Active' | 'Draft' | 'Deprecated';

export interface ToolCatalogItem {
    id: string;
    name: string;
    description: string;
    domain: string;
    type: ToolType;
    status: ToolStatus;
    version: string;
    owner: string;
    readOnly: boolean;
    policyTag: string;
    policyProfile: string;
    idempotencyRequired: boolean;
    lastUpdated: string;
}

export type ToolRiskTier = 'Low' | 'Medium' | 'High';
export type ToolApprovalMode = 'auto' | 'manual' | 'conditional';

export interface ToolPolicyProfile {
    id: string;
    name: string;
    riskTier: ToolRiskTier;
    approvalMode: ToolApprovalMode;
    redaction: 'none' | 'pii' | 'secrets';
    allowlistScope: 'project' | 'tenant';
    retryPolicy: string;
}

export interface ToolIdempotencyRule {
    id: string;
    toolId: string;
    keyStrategy: string;
    scope: 'run' | 'project' | 'tenant';
    ttl: string;
}

export type ModelProviderType = 'openai' | 'azure' | 'ollama' | 'anthropic' | 'internal';
export type ModelStatus = 'Active' | 'Degraded' | 'Offline';

export type DlqStatus = 'Failed' | 'Retrying' | 'Resolved';
export type DlqSource = 'webhook' | 'queue' | 'policy' | 'runtime';

export interface DlqEvent {
    id: string;
    eventType: string;
    source: DlqSource;
    status: DlqStatus;
    runId: string;
    tenantId: string;
    attempts: number;
    lastAttemptAt: string;
    error: string;
    payloadPreview: string;
}
export type ModelRole = 'planner' | 'worker' | 'critic' | 'router';

export interface ModelProfile {
    id: string;
    name: string;
    provider: ModelProviderType;
    modelName: string;
    role: ModelRole;
    maxTokens: number;
    temperature: number;
    timeoutSec: number;
    status: ModelStatus;
    costTier: 'Low' | 'Medium' | 'High';
    scope?: 'tenant' | 'global';
    tenantId?: string;
    endpoint?: string;
    isPrimary?: boolean;
    healthCheck?: string;
}

export type KnowledgeSourceType = 'docs' | 'db' | 'web' | 'vector' | 'graph';
export type KnowledgeStatus = 'Indexed' | 'Indexing' | 'Failed' | 'Draft';

export interface KnowledgeSource {
    id: string;
    name: string;
    type: KnowledgeSourceType;
    domain: string;
    status: KnowledgeStatus;
    lastSynced: string;
    size: string;
    owner: string;
    policyTag: string;
}

export type KnowledgePipelineStatus = 'Queued' | 'Running' | 'Success' | 'Failed';

export interface KnowledgePipelineRun {
    id: string;
    sourceId: string;
    sourceName: string;
    stage: 'ingest' | 'chunk' | 'embed' | 'index' | 'graphSync';
    status: KnowledgePipelineStatus;
    startedAt: string;
    duration: string;
    owner: string;
}

export type RunStatus = 'Running' | 'Success' | 'Failed' | 'Blocked';
export type RunPlanType = 'Deterministic' | 'Agentic';

export interface OrchestrationStep {
    id: string;
    label: string;
    status: 'queued' | 'running' | 'success' | 'failed' | 'skipped' | 'waiting';
}

export interface OrchestrationRun {
    id: string;
    project: string;
    flow: string;
    status: RunStatus;
    startedAt: string;
    duration: string;
    planType: RunPlanType;
    steps: OrchestrationStep[];
}

export type AuditSeverity = 'info' | 'warning' | 'error';

export interface OrchestrationAuditEvent {
    id: string;
    runId: string;
    event: string;
    step: string;
    severity: AuditSeverity;
    timestamp: string;
    actor: string;
    metadata?: Record<string, unknown>;
}

export type ApprovalStatus = 'Pending' | 'Approved' | 'Rejected' | 'Expired';
export type ApprovalRisk = 'Low' | 'Medium' | 'High';

export interface ApprovalRequest {
    id: string;
    runId: string;
    project: string;
    action: string;
    requestedBy: string;
    status: ApprovalStatus;
    risk: ApprovalRisk;
    policy: string;
    createdAt: string;
    expiresAt: string;
    summary: string;
}

export interface ApprovalPolicy {
    id: string;
    name: string;
    description: string;
    risk: ApprovalRisk;
    autoApprove: boolean;
    escalationTarget: string;
}

export interface ApprovalSource {
    id: string;
    name: string;
    type: 'webhook' | 'slack' | 'email' | 'dashboard';
    status: 'Active' | 'Inactive';
    destination: string;
}

export interface RunArtifact {
    id: string;
    runId: string;
    step: string;
    type: 'tool' | 'agent' | 'policy' | 'result';
    summary: string;
    payload: Record<string, unknown>;
}
