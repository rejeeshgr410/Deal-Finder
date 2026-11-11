import { db, auth } from '../firebase';
import { collection, addDoc } from 'firebase/firestore';

export interface AnalyticsEvent {
  type: 'flyer_view' | 'product_click' | 'search' | 'coupon_save' | 'retailer_page_view' | 'promotion_click' | 'ocr_metrics';
  userId: string;
  userEmail?: string;
  timestamp: number;
  details: Record<string, any>;
}

export async function trackEvent(
  type: AnalyticsEvent['type'],
  details: Record<string, any> = {}
): Promise<void> {
  try {
    const user = auth.currentUser;
    const event: AnalyticsEvent = {
      type,
      userId: user?.uid || 'anonymous',
      userEmail: user?.email || undefined,
      timestamp: Date.now(),
      details
    };

    const analyticsCol = collection(db, 'analytics');
    await addDoc(analyticsCol, event);
  } catch (error) {
    console.error('Error tracking analytics event:', error);
  }
}
