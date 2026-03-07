import { EpicResponseModel } from "../../../utils/types";
import { ErrorAction } from "../exception-handler/actions";
import { ACTION_ERROR } from "../exception-handler/types";
import { OKRActionTypes } from "./actions";
import * as actionTypes from "./types";

export interface IInitialState {
  displayScale: "day" | "week" | "month" | "quarter" | "halfYear";
  epicsLoading: boolean;
  epics: EpicResponseModel[];
  createEpicLoading: boolean;
  getEpicByIdLoading: boolean;
  epicById: EpicResponseModel | null;
  updateEpicLoading: boolean;
}

const initialState: IInitialState = {
  displayScale: "quarter",
  epics: [],
  epicsLoading: false,
  createEpicLoading: false,
  updateEpicLoading: false,
  getEpicByIdLoading: false,
  epicById: null,
};

const okrReducer = (
  state: IInitialState = initialState,
  action: OKRActionTypes | ErrorAction
): IInitialState => {
  switch (action.type) {
    case actionTypes.GET_EPICS_REQUEST:
      return {
        ...state,
        epicsLoading: true,
      };
    case actionTypes.GET_EPICS_SUCCESS:
      return {
        ...state,
        epicsLoading: false,
        epics: action.payload,
        displayScale: action.payload[0]?.displayScale,
      };
    case actionTypes.CREATE_EPIC_REQUEST:
      return {
        ...state,
        createEpicLoading: true,
      };
    case actionTypes.CREATE_EPIC_SUCCESS:
      return {
        ...state,
        createEpicLoading: false,
      };
    case actionTypes.UPDATE_EPIC_REQUEST:
      return {
        ...state,
        updateEpicLoading: true,
      };
    case actionTypes.UPDATE_EPIC_SUCCESS:
      return {
        ...state,
        updateEpicLoading: false,
      };
    case actionTypes.GET_EPIC_BY_ID_REQUEST:
      return {
        ...state,
        getEpicByIdLoading: true,
      };
    case actionTypes.GET_EPIC_BY_ID_SUCCESS:
      return {
        ...state,
        getEpicByIdLoading: false,
        epicById: action.payload,
      };
    case ACTION_ERROR:
      return {
        ...state,
        epicsLoading: false,
        createEpicLoading: false,
        getEpicByIdLoading: false,
        updateEpicLoading: false,
      };
    default:
      return state;
  }
};

export default okrReducer;
