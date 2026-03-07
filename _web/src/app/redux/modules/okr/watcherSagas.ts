import { all, takeLatest } from "redux-saga/effects";
import { exceptionHandler } from "../exception-handler";

import * as actionTypes from "./types";
import * as workerSagas from "./workerSagas";

export function* getEpicsRequestWatcher() {
  yield takeLatest(
    actionTypes.GET_EPICS_REQUEST,
    exceptionHandler(workerSagas.getEpics)
  );
}

export function* createEpicRequestWatcher() {
  yield takeLatest(
    actionTypes.CREATE_EPIC_REQUEST,
    exceptionHandler(workerSagas.createEpic)
  );
}

export function* getEpicByIDRequestWatcher() {
  yield takeLatest(
    actionTypes.GET_EPIC_BY_ID_REQUEST,
    exceptionHandler(workerSagas.getEpicById)
  );
}

export function* updateEpicRequestWatcher() {
  yield takeLatest(
    actionTypes.UPDATE_EPIC_REQUEST,
    exceptionHandler(workerSagas.updateEpic)
  );
}

export function* okrSaga() {
  yield all([
    getEpicsRequestWatcher(),
    createEpicRequestWatcher(),
    updateEpicRequestWatcher(),
    getEpicByIDRequestWatcher(),
  ]);
}
