import * as pdfjsLib from 'pdfjs-dist';
import pdfWorkerUrl from 'pdfjs-dist/build/pdf.worker.min.mjs?url';
import { createWorker } from 'tesseract.js';
import { httpsCallable } from 'firebase/functions';
import { functions } from '../firebase';

// Set up bundled PDF.js worker so extraction works offline and in Capacitor WebView
if (typeof window !== 'undefined' && pdfjsLib.GlobalWorkerOptions) {
  pdfjsLib.GlobalWorkerOptions.workerSrc = pdfWorkerUrl;
}

export interface ExtractedProductItem {
  id: string;
  name: string;
  brand: string;
  category: string;
  currentPrice: number;
  oldPrice: number;
  unitSize: string;
  unitType: string;
  packCount: number;
  discount: string;
  promoConditions?: string;
  rawSnippet?: string;
  imageUrl: string; // Cropped product image data URL
  box2d?: [number, number, number, number]; // [ymin, xmin, ymax, xmax] 0-1000
  reviewReasons: string[];
  acknowledged: boolean;
  editedFields: string[];
  modelName?: string;
  promptRevision?: string;
  sourcePage: number;
  isEdit?: boolean;
}

/**
 * Render a PDF file or URL to an array of HTML Canvas elements (one per page).
 */
export async function renderPdfToCanvases(
  pdfSource: string | File | Blob,
  maxPages: number = 10,
  scale: number = 1.8
): Promise<{ canvases: HTMLCanvasElement[]; totalPages: number }> {
  let docData: any;

  if (typeof pdfSource === 'string') {
    docData = { url: pdfSource };
  } else {
    const arrayBuffer = await pdfSource.arrayBuffer();
    docData = { data: new Uint8Array(arrayBuffer) };
  }

  const pdf = await pdfjsLib.getDocument(docData).promise;
  const totalPages = pdf.numPages;
  const pagesToRender = Math.min(totalPages, maxPages);
  const canvases: HTMLCanvasElement[] = [];

  for (let pageNum = 1; pageNum <= pagesToRender; pageNum++) {
    const page = await pdf.getPage(pageNum);
    const viewport = page.getViewport({ scale });
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    if (!ctx) continue;

    canvas.width = viewport.width;
    canvas.height = viewport.height;

    await page.render({ canvasContext: ctx, viewport } as any).promise;
    canvases.push(canvas);
  }

  return { canvases, totalPages };
}

/**
 * Crop a product image from the flyer canvas using normalized bounding box [ymin, xmin, ymax, xmax] (0-1000).
 */
export function cropProductFromCanvas(
  canvas: HTMLCanvasElement,
  box2d: [number, number, number, number],
  paddingRatio: number = 0.02
): string {
  const [ymin, xmin, ymax, xmax] = box2d;
  const cw = canvas.width;
  const ch = canvas.height;

  // Add padding while staying inside canvas bounds
  const padX = (xmax - xmin) * paddingRatio;
  const padY = (ymax - ymin) * paddingRatio;

  const left = Math.max(0, Math.floor(((xmin - padX) / 1000) * cw));
  const top = Math.max(0, Math.floor(((ymin - padY) / 1000) * ch));
  const right = Math.min(cw, Math.ceil(((xmax + padX) / 1000) * cw));
  const bottom = Math.min(ch, Math.ceil(((ymax + padY) / 1000) * ch));

  const width = Math.max(30, right - left);
  const height = Math.max(30, bottom - top);

  const cropCanvas = document.createElement('canvas');
  cropCanvas.width = width;
  cropCanvas.height = height;
  const cropCtx = cropCanvas.getContext('2d');

  if (!cropCtx) return '';

  cropCtx.fillStyle = '#ffffff';
  cropCtx.fillRect(0, 0, width, height);
  cropCtx.drawImage(canvas, left, top, width, height, 0, 0, width, height);

  try {
    return cropCanvas.toDataURL('image/webp', 0.85);
  } catch (e) {
    return cropCanvas.toDataURL('image/jpeg', 0.85);
  }
}

/**
 * Extract products and bounding boxes via trusted backend Cloud Function (`extractFlyerPage`).
 * No Gemini API key is stored or invoked from the browser.
 */
export async function extractWithGeminiVision(
  canvas: HTMLCanvasElement,
  retailerId: string,
  flyerId: string,
  pageNumber: number = 1
): Promise<ExtractedProductItem[]> {
  // Get base64 JPEG from canvas (max 1600px dimension for fast transmission)
  const maxDim = 1600;
  let exportCanvas = canvas;
  if (canvas.width > maxDim || canvas.height > maxDim) {
    const scale = Math.min(maxDim / canvas.width, maxDim / canvas.height);
    exportCanvas = document.createElement('canvas');
    exportCanvas.width = Math.round(canvas.width * scale);
    exportCanvas.height = Math.round(canvas.height * scale);
    const ctx = exportCanvas.getContext('2d');
    ctx?.drawImage(canvas, 0, 0, exportCanvas.width, exportCanvas.height);
  }

  const base64Data = exportCanvas.toDataURL('image/jpeg', 0.85).replace(/^data:image\/jpeg;base64,/, '');

  const extractCallable = httpsCallable<
    {
      retailerId: string;
      flyerId: string;
      pageNumber: number;
      mimeType: 'image/jpeg';
      imageBase64: string;
    },
    {
      products: Array<{
        name: string;
        brand: string;
        category: string;
        currentPrice: number;
        oldPrice: number;
        unitSize: string;
        unitType: string;
        discount: string;
        promoConditions: string;
        box2d: [number, number, number, number] | null;
        sourcePage: number;
        reviewReasons: string[];
        modelName: string;
        promptRevision: string;
      }>;
    }
  >(functions, 'extractFlyerPage');

  const res = await extractCallable({
    retailerId,
    flyerId,
    pageNumber,
    mimeType: 'image/jpeg',
    imageBase64: base64Data
  });

  const rawProducts = res.data?.products || [];

  return rawProducts.map((p, idx) => {
    const validBox = Array.isArray(p.box2d) && p.box2d.length === 4 ? p.box2d : undefined;
    const croppedImage = validBox ? cropProductFromCanvas(canvas, validBox) : '';
    const reasons = Array.isArray(p.reviewReasons) ? [...p.reviewReasons] : [];
    if (!validBox && !reasons.includes('BOUNDING_BOX_INVALID')) {
      reasons.push('BOUNDING_BOX_INVALID');
    }

    return {
      id: `gemini-${pageNumber}-${idx}`,
      name: p.name || 'Unnamed Product',
      brand: p.brand || '',
      category: p.category || 'General',
      currentPrice: typeof p.currentPrice === 'number' ? p.currentPrice : 0,
      oldPrice: typeof p.oldPrice === 'number' ? p.oldPrice : 0,
      unitSize: p.unitSize ? String(p.unitSize) : '',
      unitType: p.unitType || '',
      packCount: 1,
      discount: p.discount || '',
      promoConditions: p.promoConditions || '',
      imageUrl: croppedImage,
      box2d: validBox,
      reviewReasons: reasons,
      acknowledged: reasons.length === 0,
      editedFields: [],
      modelName: p.modelName,
      promptRevision: p.promptRevision,
      sourcePage: pageNumber,
      isEdit: false
    };
  });
}

/**
 * 100% Free Client-Side Offline Extraction using Tesseract.js & Smart Grid Slicing.
 */
export async function extractWithSmartGridOcr(
  canvas: HTMLCanvasElement,
  pageNumber: number = 1,
  onProgress?: (msg: string) => void
): Promise<ExtractedProductItem[]> {
  onProgress?.('Initializing client-side OCR engine...');

  const worker = await createWorker('eng', 1, {
    logger: m => {
      if (m.status === 'recognizing text') {
        onProgress?.(`Extracting text & prices... ${Math.round((m.progress || 0) * 100)}%`);
      } else {
        onProgress?.(m.status);
      }
    }
  } as any);

  onProgress?.('Analyzing flyer page layout...');
  const { data } = await worker.recognize(canvas);
  await worker.terminate();

  const rawData: any = data;
  const lines: any[] = rawData.lines || rawData.blocks?.flatMap((b: any) => b.paragraphs?.flatMap((p: any) => p.lines)) || [];
  const textLines = lines.map((l: any) => ({
    text: (l.text || '').trim(),
    bbox: l.bbox || { x0: 0, y0: 0, x1: 100, y1: 100 }
  })).filter((l: any) => l.text.length > 2);

  // Find price lines with AED / DHS or pure numbers like 10.50
  const priceRegex = /(?:AED|DHS?|DH)?\s*(\d{1,4}(?:\.\d{1,2})?)/i;
  const items: ExtractedProductItem[] = [];

  // Group into grid regions around price tags
  const cw = canvas.width;
  const ch = canvas.height;

  for (let i = 0; i < textLines.length; i++) {
    const item = textLines[i];
    const match = item.text.match(priceRegex);
    const num = match ? parseFloat(match[1]) : 0;

    // Filter reasonable supermarket prices (AED 1.00 to AED 9999.00)
    if (match && num >= 1.0 && num <= 9999) {
      // Find nearby lines above this price for the product name
      let nameCandidate = '';
      if (i > 0 && textLines[i - 1].text.length > 3) {
        nameCandidate = textLines[i - 1].text;
      }
      if (!nameCandidate || nameCandidate.match(priceRegex)) {
        nameCandidate = item.text.replace(priceRegex, '').trim() || 'Supermarket Deal';
      }

      // Calculate approximate bounding box for this product cell on the canvas
      const px = item.bbox.x0;
      const py = item.bbox.y0;

      // Estimated card size: roughly 40% of page width and 25% of height
      const cardW = Math.round(cw * 0.45);
      const cardH = Math.round(ch * 0.28);

      const cellLeft = Math.max(0, px - Math.round(cardW * 0.2));
      const cellTop = Math.max(0, py - Math.round(cardH * 0.6));
      const cellRight = Math.min(cw, cellLeft + cardW);
      const cellBottom = Math.min(ch, cellTop + cardH);

      const box2d: [number, number, number, number] = [
        Math.round((cellTop / ch) * 1000),
        Math.round((cellLeft / cw) * 1000),
        Math.round((cellBottom / ch) * 1000),
        Math.round((cellRight / cw) * 1000)
      ];

      const cropped = cropProductFromCanvas(canvas, box2d);

      items.push({
        id: `ocr-${pageNumber}-${items.length}`,
        name: nameCandidate,
        brand: '',
        category: 'General',
        currentPrice: num,
        oldPrice: 0,
        unitSize: '',
        unitType: '',
        packCount: 1,
        discount: '',
        rawSnippet: item.text,
        imageUrl: cropped,
        box2d,
        reviewReasons: ['OCR_ONLY_FALLBACK', 'UNIT_OR_PACK_UNSPECIFIED'],
        acknowledged: false,
        editedFields: [],
        sourcePage: pageNumber,
        isEdit: false
      });
    }
  }

  // Fallback if no specific prices found: generate standard 2x3 grid crops
  if (items.length === 0) {
    const rows = 3;
    const cols = 2;
    for (let r = 0; r < rows; r++) {
      for (let c = 0; c < cols; c++) {
        const ymin = Math.round(((r * 0.3) + 0.05) * 1000);
        const ymax = Math.round((((r + 1) * 0.3) + 0.05) * 1000);
        const xmin = Math.round(((c * 0.45) + 0.05) * 1000);
        const xmax = Math.round((((c + 1) * 0.45) + 0.05) * 1000);
        const box2d: [number, number, number, number] = [ymin, xmin, ymax, xmax];
        const cropped = cropProductFromCanvas(canvas, box2d);

        items.push({
          id: `grid-${pageNumber}-${items.length}`,
          name: `Catalogue Item ${items.length + 1}`,
          brand: '',
          category: 'General',
          currentPrice: 0.0,
          oldPrice: 0.0,
          unitSize: '',
          unitType: '',
          packCount: 1,
          discount: '',
          imageUrl: cropped,
          box2d,
          reviewReasons: ['OCR_ONLY_FALLBACK', 'PRICE_ZERO_OR_MISSING', 'UNIT_OR_PACK_UNSPECIFIED', 'GRID_CROP_CANDIDATE'],
          acknowledged: false,
          editedFields: [],
          sourcePage: pageNumber,
          isEdit: false
        });
      }
    }
  }

  return items;
}

/**
 * Universal auto-extraction function:
 * Uses trusted backend Gemini Vision when available,
 * otherwise falls back to client-side Smart Grid OCR (OCR-only).
 */
export async function extractProductsFromCanvas(
  canvas: HTMLCanvasElement,
  retailerId: string,
  flyerId: string,
  pageNumber: number = 1,
  onProgress?: (msg: string) => void
): Promise<{ products: ExtractedProductItem[]; engineUsed: 'gemini' | 'tesseract' }> {
  if (retailerId && flyerId) {
    try {
      onProgress?.('Extracting products via protected server AI pipeline...');
      const products = await extractWithGeminiVision(canvas, retailerId, flyerId, pageNumber);
      if (products && products.length > 0) {
        return { products, engineUsed: 'gemini' };
      }
    } catch (err: any) {
      console.warn('Backend Gemini extraction unavailable, falling back to local OCR:', err?.message);
      onProgress?.('Server AI unavailable or rate limited. Falling back to local OCR (OCR-only)...');
    }
  }

  // Offline / OCR-only fallback
  const products = await extractWithSmartGridOcr(canvas, pageNumber, onProgress);
  return { products, engineUsed: 'tesseract' };
}
