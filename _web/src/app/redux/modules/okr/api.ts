import axios, { AxiosResponse } from "axios";
import {
  CreateEpicRequestModel,
  EpicRequestModel,
  EpicResponseModel,
  UpdateEpicRequestModel,
} from "../../../utils/types";

export const getEpics = ({
  projectKey,
  startDate,
  endDate,
  statusFilter,
  assignee,
  includeKeyResults,
  includeMilestones,
  displayScale,
}: EpicRequestModel): Promise<AxiosResponse<EpicResponseModel[]>> => {
  return axios({
    method: "GET",
    url: `/get-epics?projectKey=${projectKey}&startDate=${startDate}&endDate=${endDate}&statusFilter=${statusFilter}&assignee=${assignee}&includeKeyResults=${includeKeyResults}&includeMilestones=${includeMilestones}&displayScale=${displayScale}`,
    /* url: `/get-epics`, */
    data: null,
  });
};

export const createEpic = ({
  projectKey,
  issueType,
  summary,
  description,
  priority,
  startDate,
  endDate,
  assignee,
  keyResults,
  milestones,
}: CreateEpicRequestModel): Promise<AxiosResponse<EpicResponseModel>> => {
  return axios({
    method: "POST",
    url: "/create-epic",
    data: {
      projectKey,
      issueType,
      summary,
      description,
      priority,
      startDate,
      endDate,
      assignee,
      keyResults,
      milestones,
    },
  });
};

export const updateEpic = ({
  id,
  projectKey,
  issueType,
  summary,
  description,
  priority,
  startDate,
  endDate,
  assignee,
  keyResults,
  milestones,
}: UpdateEpicRequestModel): Promise<AxiosResponse<string>> => {
  return axios({
    method: "POST",
    url: "/update-epic",
    data: {
      id,
      projectKey,
      issueType,
      summary,
      description,
      priority,
      startDate,
      endDate,
      assignee,
      keyResults,
      milestones,
    },
  });
};

export const getEpicByID = (
  id: string
): Promise<AxiosResponse<EpicResponseModel>> => {
  return axios({
    method: "POST",
    url: "/get-epic-by-id",
    data: id,
  });
};
