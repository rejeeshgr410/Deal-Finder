import { auth, functions } from '../firebase';
import { httpsCallable } from 'firebase/functions';

export interface AnalyticsEvent {
  type: 'flyer_view' | 'product_click' | 'search' | 'coupon_save' | 'retailer_page_view' | 'promotion_click' | 'ocr_metrics';
  userId: string;
  timestamp: number;
  details: Record<string, any>;
}

const PII_KEYS = new Set(['email', 'userEmail', 'phone', 'token', 'fcmToken', 'password']);

export async function trackEvent(
  type: AnalyticsEvent['type'],
  details: Record<string, any> = {},
  tenantId?: string
): Promise<void> {
  if (!auth.currentUser) return;

  const cleanDetails: Record<string, string | number | boolean | null> = {};
  for (const [k, v] of Object.entries(details)) {
    if (PII_KEYS.has(k)) continue;
    if (typeof v === 'string' || typeof v === 'number' || typeof v === 'boolean' || v === null) {
      cleanDetails[k] = typeof v === 'string' ? v.slice(0, 200) : v;
    }
  }

  try {
    const callable = httpsCallable(functions, 'trackAnalyticsEvent');
    await callable({
      type,
      tenantId,
      details: cleanDetails
    });
  } catch (error) {
    // Non-blocking telemetry; never throw or leak PII
    console.warn('Analytics event not recorded (backend unavailable):', error);
  }
}
