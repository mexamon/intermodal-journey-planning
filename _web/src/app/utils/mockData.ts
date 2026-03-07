import { EpicResponseModel } from "./types";
import { v4 as uuidv4 } from "uuid";

export const mockRoadMapData: EpicResponseModel[] = [
  {
    id: uuidv4(),
    displayScale: "day",
    project: {
      id: uuidv4(),
      projectKey: "BU-1",
      projectName: "Boilerrum UI",
      startDate: "2024-01-01",
      endDate: "2024-12-12",
    },
    objectives: [
      {
        id: "TET-1",
        issueKey: "TET-1",
        summary: "Increase revenue by 10%",
        description: "Focus on customer acquisition",
        status: "In Progress",
        startDate: "2024-09-11",
        endDate: "2024-09-22",
        assignee: "john.doe",
        progressPercentage: 60,
        keyResults: [
          {
            id: uuidv4(),
            keyResultID: "KR-101",
            summary: "Acquire 1000 new customers",
            targetValue: "1000",
            currentValue: "600",
            status: "In Progress",
            dueDate: "2024-05-31",
          },
        ],
        milestones: [
          {
            id: uuidv4(),
            milestoneID: "MILE-100",
            title: "Launch marketing campaign",
            dueDate: "2024-09-09",
            isAchieved: false,
          },
        ],
      },
      {
        id: "TET-2",
        issueKey: "TET-2",
        summary: "Designing mockups",
        description: "Designing mockups for another project",
        status: "To Do",
        startDate: "2024-10-02",
        endDate: "2024-10-06",
        assignee: "john.doe",
        progressPercentage: 60,
        keyResults: [
          {
            id: uuidv4(),
            keyResultID: "KR-102",
            summary: "Design at least two screens",
            targetValue: "1000",
            currentValue: "600",
            status: "In Progress",
            dueDate: "2024-05-31",
          },
        ],
        milestones: [
          {
            id: uuidv4(),
            milestoneID: "MILE-100",
            title: "Design first screen",
            dueDate: "2024-09-12",
            isAchieved: true,
          },
        ],
      },
      {
        id: "TET-3",
        issueKey: "TET-3",
        summary: "Test Epic",
        description: "Designing mockups for another project",
        status: "To Do",
        startDate: "2024-10-08",
        endDate: "2024-10-22",
        assignee: "john.doe",
        progressPercentage: 60,
        keyResults: [
          {
            id: uuidv4(),
            keyResultID: "KR-102",
            summary: "Design at least two screens",
            targetValue: "1000",
            currentValue: "600",
            status: "In Progress",
            dueDate: "2024-05-31",
          },
        ],
        milestones: [
          {
            id: uuidv4(),
            milestoneID: "MILE-100",
            title: "Design first screen",
            dueDate: "2024-09-12",
            isAchieved: true,
          },
        ],
      },
    ],
    totalObjectives: 5,
    completedObjectives: 2,
    inProgessObjectives: 3,
  },
];
