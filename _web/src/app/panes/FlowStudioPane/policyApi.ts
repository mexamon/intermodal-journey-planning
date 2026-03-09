import { apiGet, apiPost, apiPut, apiDelete } from '../../api/client';

/* ═══════════════════════════════════════════════
   Policy API Types — matching backend DTOs & entities
   ═══════════════════════════════════════════════ */

/** Response type — entities from backend (enums serialized as {value, desc}) */
export interface PolicySetDto {
  id: string;
  code: string;
  scopeType: { value: string; desc: string };
  scopeKey: string;
  segment: { value: string; desc: string };
  status: { value: string; desc: string };
  description?: string;
  effectiveFrom?: string;
  effectiveTo?: string;
  createdDate?: string;
  lastModifiedDate?: string;
}

/** Request type — plain string values for enums (matches PolicySetRequest DTO) */
export interface PolicySetCreateRequest {
  code: string;
  scopeType: string;   // "GLOBAL" | "COUNTRY" | "REGION" | "AIRPORT" | "AIRPORT_PAIR"
  scopeKey: string;
  segment: string;     // "DEFAULT" | "CORPORATE" | "ELITE" | "IRROPS"
  status: string;      // "DRAFT" | "ACTIVE" | "DEPRECATED"
  description?: string;
}

export interface ConstraintsDto {
  id: string;
  maxLegs: number;
  minFlights: number;
  maxFlights: number;
  minTransfers: number;
  maxTransfers: number;
  maxTotalDurationMin?: number;
  maxWalkingTotalM?: number;
  minConnectionMinutes?: number;
  maxTotalCo2Grams?: number;
}

export interface ConstraintsRequest {
  maxLegs: number;
  minFlights: number;
  maxFlights: number;
  minTransfers: number;
  maxTransfers: number;
  maxTotalDurationMin?: number;
  maxWalkingTotalM?: number;
  minConnectionMinutes?: number;
  maxTotalCo2Grams?: number;
}

export interface PolicyNodeDto {
  id: string;
  nodeKey: { value: string; desc: string };
  minVisits: number;
  maxVisits: number;
  propsJson?: string;
  uiX?: number;
  uiY?: number;
}

export interface NodeRequest {
  nodeKey: string;     // "START" | "BEFORE" | "FLIGHT" | "AFTER" | "END" | "WALK_ACCESS"
  minVisits: number;
  maxVisits: number;
  propsJson?: string;
  uiX?: number;
  uiY?: number;
}

export interface PolicyTransitionDto {
  id: string;
  fromNode: { id: string };
  toNode: { id: string };
  priority: number;
  guardJson?: string;
  uiJson?: string;
}

export interface TransitionRequest {
  fromNodeId: string;
  toNodeId: string;
  priority: number;
  guardJson?: string;
  uiJson?: string;
}

/* ═══════════════════════════════════════════════
   Page response from Spring Data
   ═══════════════════════════════════════════════ */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/* ═══════════════════════════════════════════════
   Policy Set CRUD
   ═══════════════════════════════════════════════ */

export async function searchPolicySets(
  filters: {
    code?: string;
    scopeType?: string;
    scopeKey?: string;
    segment?: string;
    status?: string;
  } = {},
  page = 0,
  size = 50
): Promise<PageResponse<PolicySetDto>> {
  return apiPost<PageResponse<PolicySetDto>>(
    `/policy/sets/search?page=${page}&size=${size}&sort=createdDate,desc`,
    filters
  );
}

export async function getPolicySet(id: string): Promise<PolicySetDto> {
  return apiGet<PolicySetDto>(`/policy/sets/${id}`);
}

export async function createPolicySet(data: PolicySetCreateRequest): Promise<PolicySetDto> {
  return apiPost<PolicySetDto>('/policy/sets', data);
}

export async function updatePolicySet(id: string, data: PolicySetCreateRequest): Promise<PolicySetDto> {
  return apiPut<PolicySetDto>(`/policy/sets/${id}`, data);
}

export async function deletePolicySet(id: string): Promise<void> {
  return apiDelete<void>(`/policy/sets/${id}`);
}

/* ═══════════════════════════════════════════════
   Constraints
   ═══════════════════════════════════════════════ */

export async function getConstraints(policySetId: string): Promise<ConstraintsDto | null> {
  return apiGet<ConstraintsDto | null>(`/policy/sets/${policySetId}/constraints`);
}

export async function saveConstraints(policySetId: string, data: ConstraintsRequest): Promise<ConstraintsDto> {
  return apiPut<ConstraintsDto>(`/policy/sets/${policySetId}/constraints`, data);
}

/* ═══════════════════════════════════════════════
   Nodes
   ═══════════════════════════════════════════════ */

export async function listNodes(policySetId: string): Promise<PolicyNodeDto[]> {
  return apiGet<PolicyNodeDto[]>(`/policy/sets/${policySetId}/nodes`);
}

export async function saveNodes(policySetId: string, nodes: NodeRequest[]): Promise<PolicyNodeDto[]> {
  return apiPut<PolicyNodeDto[]>(`/policy/sets/${policySetId}/nodes`, nodes);
}

/* ═══════════════════════════════════════════════
   Transitions
   ═══════════════════════════════════════════════ */

export async function listTransitions(policySetId: string): Promise<PolicyTransitionDto[]> {
  return apiGet<PolicyTransitionDto[]>(`/policy/sets/${policySetId}/transitions`);
}

export async function saveTransitions(policySetId: string, transitions: TransitionRequest[]): Promise<PolicyTransitionDto[]> {
  return apiPut<PolicyTransitionDto[]>(`/policy/sets/${policySetId}/transitions`, transitions);
}
