import { combineReducers } from "redux";

import okrReducer from "../modules/okr/reducers";

const rootReducer = combineReducers({
  okr: okrReducer,
});

export type RootReducerState = ReturnType<typeof rootReducer>;

export default rootReducer;
