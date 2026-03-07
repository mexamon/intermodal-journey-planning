import { EpicRequestModel, EpicResponseModel } from "./types";

interface IFilterData extends EpicRequestModel {
  data: EpicResponseModel[];
}

export const filterData = ({
  data,
  projectKey,
  startDate,
  endDate,
  statusFilter,
  assignee,
  includeKeyResults,
  includeMilestones,
  displayScale,
}: IFilterData): EpicResponseModel[] => {
  console.log(
    "CONDITION=>",
    projectKey === null &&
      startDate === null &&
      endDate === null &&
      statusFilter === null &&
      assignee === null &&
      includeKeyResults === null &&
      includeMilestones === null
  );
  if (
    projectKey === null &&
    startDate === null &&
    endDate === null &&
    statusFilter === null &&
    assignee === null &&
    includeKeyResults === null &&
    includeMilestones === null &&
    displayScale === null
  ) {
    console.log("U");
    return data;
  }

  return data.filter((project) => {
    const projectMatches =
      (!projectKey || project.project.projectKey === projectKey) &&
      (!startDate || project.project.startDate >= startDate) &&
      (!endDate || project.project.endDate <= endDate);

    const objectivesMatches = project.objectives.filter((objective) => {
      const statusMatches = !statusFilter || objective.status === statusFilter;
      const assigneeMatches = !assignee || objective.assignee === assignee;

      const keyResultsMatches =
        includeKeyResults === null ||
        (includeKeyResults &&
          objective.keyResults &&
          objective.keyResults.length > 0);

      const milestonesMatches =
        includeMilestones === null ||
        (includeMilestones &&
          objective.milestones &&
          objective.milestones.length > 0);

      return (
        statusMatches &&
        assigneeMatches &&
        keyResultsMatches &&
        milestonesMatches
      );
    });

    return projectMatches && objectivesMatches.length > 0;
  });
};
