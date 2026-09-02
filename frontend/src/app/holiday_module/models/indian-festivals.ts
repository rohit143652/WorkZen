/**
 * Reference-only Indian festival/holiday dates shown on the calendar as a visual hint (small
 * colored dot + name in the tooltip) - these are NOT actual company holidays and do nothing on
 * their own; an admin still has to explicitly click the date and "Add Holiday" for it to affect
 * attendance/payroll at all.
 *
 * FIXED_HOLIDAYS repeat on the same Gregorian date every year (national holidays), computed
 * for whatever year the calendar is currently showing.
 *
 * VARIABLE_FESTIVALS are lunar/religious festivals whose date shifts year to year - these are
 * hardcoded per-year best-effort estimates (some depend on moon sighting and can shift by a day
 * in practice). Only 2025-2027 are covered; verify against an official calendar before relying
 * on these for anything but a rough visual reference, and extend this list for later years as
 * they approach.
 */

export interface FestivalRef {
  name: string;
  /** 'national' (fixed date, statutory) or 'festival' (variable, religious/cultural) - used only to pick a slightly different dot color. */
  kind: 'national' | 'festival';
}

/** Keyed by "MM-DD" - applies to every year. */
const FIXED_HOLIDAYS: Record<string, string> = {
  '01-01': "New Year's Day",
  '01-26': 'Republic Day',
  '08-15': 'Independence Day',
  '10-02': 'Gandhi Jayanti',
  '12-25': 'Christmas'
};

/** Keyed by exact "YYYY-MM-DD" - best-effort estimates, verify before relying on these. */
const VARIABLE_FESTIVALS: Record<string, string> = {
  // 2025
  '2025-01-14': 'Makar Sankranti',
  '2025-03-14': 'Holi',
  '2025-03-30': 'Eid-ul-Fitr',
  '2025-04-06': 'Ram Navami',
  '2025-06-07': 'Eid-ul-Adha (Bakrid)',
  '2025-08-09': 'Raksha Bandhan',
  '2025-08-16': 'Janmashtami',
  '2025-08-27': 'Ganesh Chaturthi',
  '2025-10-02': 'Dussehra',
  '2025-10-20': 'Diwali',
  '2025-11-05': 'Guru Nanak Jayanti',
  // 2026
  '2026-01-14': 'Makar Sankranti',
  '2026-03-04': 'Holi',
  '2026-03-20': 'Eid-ul-Fitr',
  '2026-03-26': 'Ram Navami',
  '2026-05-27': 'Eid-ul-Adha (Bakrid)',
  '2026-08-28': 'Raksha Bandhan',
  '2026-09-04': 'Janmashtami',
  '2026-09-14': 'Ganesh Chaturthi',
  '2026-10-20': 'Dussehra',
  '2026-11-08': 'Diwali',
  '2026-11-24': 'Guru Nanak Jayanti',
  // 2027
  '2027-01-14': 'Makar Sankranti',
  '2027-03-21': 'Eid-ul-Fitr',
  '2027-03-22': 'Holi',
  '2027-04-15': 'Ram Navami',
  '2027-05-16': 'Eid-ul-Adha (Bakrid)',
  '2027-08-17': 'Raksha Bandhan',
  '2027-08-24': 'Janmashtami',
  '2027-09-03': 'Ganesh Chaturthi',
  '2027-10-09': 'Dussehra',
  '2027-10-28': 'Diwali',
  '2027-11-13': 'Guru Nanak Jayanti'
};

/** dateStr must be "YYYY-MM-DD". Returns undefined if this date isn't a known reference festival. */
export function getFestivalRef(dateStr: string): FestivalRef | undefined {
  const monthDay = dateStr.slice(5); // "MM-DD"
  const fixedName = FIXED_HOLIDAYS[monthDay];
  if (fixedName) return { name: fixedName, kind: 'national' };

  const variableName = VARIABLE_FESTIVALS[dateStr];
  if (variableName) return { name: variableName, kind: 'festival' };

  return undefined;
}

/** Every reference festival/national-day date for one calendar year - used by "Add Year's
    Holidays" to offer a full year's worth of suggestions in one go, instead of a Client Admin
    manually adding each one date-by-date. Still just a reference list; nothing here becomes an
    actual company Holiday until explicitly submitted through that bulk-add flow. */
export function getFestivalsForYear(year: number): { date: string; name: string }[] {
  const result: { date: string; name: string }[] = [];
  for (const [monthDay, name] of Object.entries(FIXED_HOLIDAYS)) {
    result.push({ date: `${year}-${monthDay}`, name });
  }
  for (const [dateStr, name] of Object.entries(VARIABLE_FESTIVALS)) {
    if (dateStr.startsWith(`${year}-`)) {
      result.push({ date: dateStr, name });
    }
  }
  return result.sort((a, b) => a.date.localeCompare(b.date));
}
