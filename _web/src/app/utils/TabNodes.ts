import { ReactNode } from "react";

type NodeMapping = {
  key: string;
  label: string;
  children: ReactNode;
};

export type PathSegments = {
  users: NodeMapping;
  domains: NodeMapping;
  roles: NodeMapping;
  companies: NodeMapping;
};

export const nodeMapping: PathSegments = {
  users: {
    key: "users",
    label: "Users",
    children: "TEST USERS",
  },
  domains: {
    key: "domains",
    label: "Domains",
    children: "TEST Domains",
  },
  roles: {
    key: "roles",
    label: "Roles",
    children: "ROLES CONTENT",
  },
  companies: {
    key: "companies",
    label: "Companies",
    children: "COMPANIES CONTENT",
  },
};
