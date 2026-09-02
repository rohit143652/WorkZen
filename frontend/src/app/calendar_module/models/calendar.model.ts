export type CalendarItemType = 'EVENT' | 'HOLIDAY';
export type EventVisibility = 'ALL_USERS' | 'SELECTED_USERS';

export interface CalendarItemResponse {
  id: number;
  type: CalendarItemType;
  title: string;
  description?: string;
  location?: string;
  startAt: string;
  endAt: string;
  allDay: boolean;
  // EVENT-only
  visibility?: EventVisibility;
  participantEmployeeIds?: number[];
  createdByName?: string;
  // HOLIDAY-only
  companyWide?: boolean;
}

export interface EventRequest {
  title: string;
  description?: string;
  location?: string;
  startAt: string;
  endAt: string;
  allDay: boolean;
  visibility: EventVisibility;
  participantEmployeeIds?: number[];
}

export type CalendarViewMode = 'day' | 'week' | 'month';
