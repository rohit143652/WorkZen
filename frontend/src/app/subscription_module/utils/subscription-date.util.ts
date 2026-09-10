/**
 * Auto-calculates a subscription's end date from its start date and billing cycle - Monthly
 * adds exactly one month, Yearly adds exactly one year, Custom is left for the admin to set
 * manually (there's no single "custom" duration to derive it from).
 *
 * Returns a plain 'YYYY-MM-DD' string, matching what a native <input type="date"> control
 * expects/produces, so callers can assign the result straight into a form control's value.
 */
export function calculateSubscriptionEndDate(startDate: string, billingCycle: string): string | null {
  if (!startDate) return null;
  const start = new Date(startDate + 'T00:00:00');
  if (isNaN(start.getTime())) return null;

  const end = new Date(start);
  if (billingCycle === 'MONTHLY') {
    end.setMonth(end.getMonth() + 1);
  } else if (billingCycle === 'YEARLY') {
    end.setFullYear(end.getFullYear() + 1);
  } else {
    return null; // CUSTOM - no fixed duration to derive, admin sets it manually
  }
  return end.toISOString().slice(0, 10);
}
