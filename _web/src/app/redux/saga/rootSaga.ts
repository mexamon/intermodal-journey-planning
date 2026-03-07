import { all, fork } from "redux-saga/effects";

import { okrSaga } from "../modules/okr/watcherSagas";

export default function* rootSaga() {
  yield all([fork(okrSaga)]);
}
