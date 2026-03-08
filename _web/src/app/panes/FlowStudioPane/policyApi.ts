import { apiGet, apiPost, apiPut, apiDelete } from '../../api/client';

/* ═══════════════════════════════════════════════
   Policy API Types — matching backend entities
   ═══════════════════════════════════════════════ */

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

export interface PolicyConstraintsDto {
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
  preferredModesJson?: string;
  constraintsJson?: string;
}

export interface PolicyNodeDto {
  id?: string;
  nodeKey: { value: string; desc: string };
  minVisits: number;
  maxVisits: number;
  propsJson?: string;
  uiX?: number;
  uiY?: number;
}

export interface PolicyTransitionDto {
  id?: string;
  fromNode: { id: string };
  toNode: { id: string };
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

/** Search policy sets with filters */
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

/** Get a single policy set by ID */
export async function getPolicySet(id: string): Promise<PolicySetDto> {
  return apiGet<PolicySetDto>(`/policy/sets/${id}`);
}

/** Create a new policy set */
export async function createPolicySet(data: Partial<PolicySetDto>): Promise<PolicySetDto> {
  return apiPost<PolicySetDto>('/policy/sets', data);
}

/** Update an existing policy set */
export async function updatePolicySet(id: string, data: Partial<PolicySetDto>): Promise<PolicySetDto> {
  return apiPut<PolicySetDto>(`/policy/sets/${id}`, data);
}

/** Delete a policy set */
export async function deletePolicySet(id: string): Promise<void> {
  return apiDelete<void>(`/policy/sets/${id}`);
}

/* ═══════════════════════════════════════════════
   Constraints
   ═══════════════════════════════════════════════ */

export async function getConstraints(policySetId: string): Promise<PolicyConstraintsDto> {
  return apiGet<PolicyConstraintsDto>(`/policy/sets/${policySetId}/constraints`);
}

export async function saveConstraints(policySetId: string, data: Partial<PolicyConstraintsDto>): Promise<PolicyConstraintsDto> {
  return apiPut<PolicyConstraintsDto>(`/policy/sets/${policySetId}/constraints`, data);
}

/* ═══════════════════════════════════════════════
   Nodes
   ═══════════════════════════════════════════════ */

export async function listNodes(policySetId: string): Promise<PolicyNodeDto[]> {
  return apiGet<PolicyNodeDto[]>(`/policy/sets/${policySetId}/nodes`);
}

export async function saveNodes(policySetId: string, nodes: PolicyNodeDto[]): Promise<PolicyNodeDto[]> {
  return apiPut<PolicyNodeDto[]>(`/policy/sets/${policySetId}/nodes`, nodes);
}

/* ═══════════════════════════════════════════════
   Transitions
   ═══════════════════════════════════════════════ */

export async function listTransitions(policySetId: string): Promise<PolicyTransitionDto[]> {
  return apiGet<PolicyTransitionDto[]>(`/policy/sets/${policySetId}/transitions`);
}

export async function saveTransitions(policySetId: string, transitions: PolicyTransitionDto[]): Promise<PolicyTransitionDto[]> {
  return apiPut<PolicyTransitionDto[]>(`/policy/sets/${policySetId}/transitions`, transitions);
}
