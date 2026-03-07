import * as actionTypes from './types';

export interface ActionError {
  type: typeof actionTypes.ACTION_ERROR;
  payload: any;
}

export const actionError = (payload: any): ActionError => {
  return {
    type: actionTypes.ACTION_ERROR,
    payload,
  };
};

export type ErrorAction = ActionError;
