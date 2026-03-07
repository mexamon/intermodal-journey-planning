import { Navigate, Route, Routes as Switch } from "react-router-dom";
import { RoadMapScreen } from "./pages";

const Routes = () => {
  return (
    <Switch>
      <Route path="/" element={<Navigate to="/roadmap" replace />} />
      <Route path="/roadmap" element={<RoadMapScreen />} />
    </Switch>
  );
};

export default Routes;
