import { call } from "redux-saga/effects";
import * as Sentry from "@sentry/react";

import * as globalActions from "./actions";
import store from "../../store/store";

const errorHandler = (error: any) => {
  store.dispatch(globalActions.actionError(error));
  console.log({ error });
  Sentry.captureException(error);

  /* console.log(error.response.data); */
};

export const exceptionHandler = (saga: any, ...args: any) =>
  function* (action: any) {
    try {
      yield call(saga, ...args, action);
    } catch (error) {
      //@ts-ignore
      yield call(errorHandler, ...args, error);
    }
  };
