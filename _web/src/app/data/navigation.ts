import type { IconType } from 'react-icons';
import {
  FiNavigation,
  FiMapPin,
  FiGlobe,
  FiGitBranch,
  FiTruck,
  FiUsers,
  FiBarChart2,
  FiLink,
  FiBox,
  FiTarget,
  FiDollarSign,
} from 'react-icons/fi';

export interface NavItem {
  id: string;
  label: string;
  description: string;
  icon: IconType;
}

export interface NavSection {
  id: string;
  label: string;
  items: NavItem[];
}

export const navSections: NavSection[] = [
  {
    id: 'plan',
    label: 'Plan',
    items: [
      {
        id: 'planner',
        label: 'Journey Planner',
        description: 'Search multi-modal routes across flights, trains, and buses.',
        icon: FiNavigation,
      },
      {
        id: 'routes',
        label: 'Saved Routes',
        description: 'Browse and manage your saved journey routes.',
        icon: FiMapPin,
      },
    ],
  },
  {
    id: 'manage',
    label: 'Manage',
    items: [
      {
        id: 'locations',
        label: 'Locations',
        description: 'Manage airports, cities, stations, and points of interest.',
        icon: FiGlobe,
      },
      {
        id: 'connections',
        label: 'Connections',
        description: 'View and manage transportation edges between locations.',
        icon: FiLink,
      },
      {
        id: 'fares',
        label: 'Fares',
        description: 'Manage pricing, fare classes, and refund policies.',
        icon: FiDollarSign,
      },
      // ── Reference data (bottom) ─────────────────
      {
        id: 'service-areas',
        label: 'Service Areas',
        description: 'Configure geographic zones for transport modes (taxi zones, uber coverage).',
        icon: FiTarget,
      },
      {
        id: 'providers',
        label: 'Providers',
        description: 'Manage airlines, railway operators, and ride-share providers.',
        icon: FiBox,
      },
      {
        id: 'modes',
        label: 'Transport Modes',
        description: 'Configure transport mode registry and sourcing strategies.',
        icon: FiTruck,
      },
    ],
  },
  {
    id: 'admin',
    label: 'Admin',
    items: [
      {
        id: 'flows',
        label: 'Policy Studio',
        description: 'Design journey policy state machines with visual flow editor.',
        icon: FiGitBranch,
      },
      {
        id: 'users',
        label: 'Users & Roles',
        description: 'Manage members, roles, and workspace access.',
        icon: FiUsers,
      },
      {
        id: 'analytics',
        label: 'Analytics',
        description: 'Monitor search activity, route usage, and system health.',
        icon: FiBarChart2,
      },
    ],
  },
];

export const navItemsFlat = navSections.flatMap((section) => section.items);
