import { call, CallEffect, put, PutEffect } from "redux-saga/effects";
import { AxiosResponse } from "axios";

import * as actions from "./actions";
import * as api from "./api";
import {
  CreateEpicRequestModel,
  EpicRequestModel,
  EpicResponseModel,
} from "../../../utils/types";

export function* getEpics(
  action: actions.GetEpicsRequestAction
): Generator<
  | CallEffect<AxiosResponse<EpicResponseModel[]>>
  | PutEffect<actions.GetEpicsSuccessAction>,
  void,
  AxiosResponse<EpicResponseModel[]>
> {
  const response: AxiosResponse<EpicResponseModel[]> = yield call(
    api.getEpics,
    action.payload
  );

  yield put(actions.getEpicsSuccess(response.data));
}

export function* createEpic(
  action: actions.CreateEpicRequestAction
): Generator<
  | CallEffect<AxiosResponse<EpicResponseModel>>
  | PutEffect<actions.CreateEpicSuccessAction>
  | PutEffect<actions.GetEpicsRequestAction>,
  void,
  AxiosResponse<EpicResponseModel>
> {
  const response: AxiosResponse<EpicResponseModel> = yield call(
    api.createEpic,
    action.payload
  );

  yield put(actions.createEpicSuccess(response.data));
  yield put<actions.GetEpicsRequestAction>({
    type: "GET_EPICS_REQUEST",
    payload: {
      projectKey: null,
      startDate: null,
      endDate: null,
      statusFilter: null,
      assignee: null,
      includeKeyResults: null,
      includeMilestones: null,
    },
  });
}

export function* getEpicById(
  action: actions.GetEpicByIDRequestAction
): Generator<
  | CallEffect<AxiosResponse<EpicResponseModel>>
  | PutEffect<actions.GetEpicByIDSuccessAction>,
  void,
  AxiosResponse<EpicResponseModel>
> {
  const response: AxiosResponse<EpicResponseModel> = yield call(
    api.getEpicByID,
    action.payload
  );

  yield put(actions.getEpicByIDSuccess(response.data));
}

export function* updateEpic(
  action: actions.UpdateEpicRequestAction
): Generator<
  | CallEffect<AxiosResponse<string>>
  | PutEffect<actions.UpdateEpicSuccessAction>
  | PutEffect<actions.GetEpicsRequestAction>,
  void,
  AxiosResponse<string>
> {
  const response: AxiosResponse<string> = yield call(
    api.updateEpic,
    action.payload
  );

  yield put(actions.updateEpicSuccess("success"));
  yield put<actions.GetEpicsRequestAction>({
    type: "GET_EPICS_REQUEST",
    payload: {
      projectKey: null,
      startDate: null,
      endDate: null,
      statusFilter: null,
      assignee: null,
      includeKeyResults: null,
      includeMilestones: null,
    },
  });
}
