import * as actionTypes from "./types";

import {
  EpicResponseModel,
  EpicRequestModel,
  CreateEpicRequestModel,
  UpdateEpicRequestModel,
} from "../../../utils/types";

export interface GetEpicsRequestAction {
  type: typeof actionTypes.GET_EPICS_REQUEST;
  payload: EpicRequestModel;
}

export interface GetEpicsSuccessAction {
  type: typeof actionTypes.GET_EPICS_SUCCESS;
  payload: EpicResponseModel[];
}

export const getEpicsRequest = (
  payload: EpicRequestModel
): GetEpicsRequestAction => {
  return {
    type: actionTypes.GET_EPICS_REQUEST,
    payload,
  };
};

export const getEpicsSuccess = (
  payload: EpicResponseModel[]
): GetEpicsSuccessAction => {
  return {
    type: actionTypes.GET_EPICS_SUCCESS,
    payload,
  };
};

/**
 * CREATE EPIC
 */

export interface CreateEpicRequestAction {
  type: typeof actionTypes.CREATE_EPIC_REQUEST;
  payload: CreateEpicRequestModel;
}

export interface CreateEpicSuccessAction {
  type: typeof actionTypes.CREATE_EPIC_SUCCESS;
  payload: EpicResponseModel;
}

export const createEpicRequest = (
  payload: CreateEpicRequestModel
): CreateEpicRequestAction => {
  return {
    type: actionTypes.CREATE_EPIC_REQUEST,
    payload,
  };
};

export const createEpicSuccess = (
  payload: EpicResponseModel
): CreateEpicSuccessAction => {
  return {
    type: actionTypes.CREATE_EPIC_SUCCESS,
    payload,
  };
};

/**
 * GET EPIC BY ID
 */

export interface GetEpicByIDRequestAction {
  type: typeof actionTypes.GET_EPIC_BY_ID_REQUEST;
  payload: string;
}

export interface GetEpicByIDSuccessAction {
  type: typeof actionTypes.GET_EPIC_BY_ID_SUCCESS;
  payload: EpicResponseModel;
}

export const getEpicByIDRequest = (
  payload: string
): GetEpicByIDRequestAction => {
  return {
    type: actionTypes.GET_EPIC_BY_ID_REQUEST,
    payload,
  };
};

export const getEpicByIDSuccess = (
  payload: EpicResponseModel
): GetEpicByIDSuccessAction => {
  return {
    type: actionTypes.GET_EPIC_BY_ID_SUCCESS,
    payload,
  };
};

/**
 * UPDATE EPIC
 */

export interface UpdateEpicRequestAction {
  type: typeof actionTypes.UPDATE_EPIC_REQUEST;
  payload: UpdateEpicRequestModel;
}

export interface UpdateEpicSuccessAction {
  type: typeof actionTypes.UPDATE_EPIC_SUCCESS;
  payload: string;
}

export const updateEpicRequest = (
  payload: UpdateEpicRequestModel
): UpdateEpicRequestAction => {
  return {
    type: actionTypes.UPDATE_EPIC_REQUEST,
    payload,
  };
};

export const updateEpicSuccess = (payload: string): UpdateEpicSuccessAction => {
  return {
    type: actionTypes.UPDATE_EPIC_SUCCESS,
    payload,
  };
};

export type OKRActionTypes =
  | GetEpicsRequestAction
  | GetEpicsSuccessAction
  | CreateEpicRequestAction
  | CreateEpicSuccessAction
  | GetEpicByIDRequestAction
  | GetEpicByIDSuccessAction
  | UpdateEpicRequestAction
  | UpdateEpicSuccessAction;
