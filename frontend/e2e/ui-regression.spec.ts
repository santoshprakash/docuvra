import { expect, Page, test } from '@playwright/test';
import path from 'path';

const screenshotsDir = path.join(process.cwd(), 'test-results', 'screenshots');
const token = 'playwright-token';

const users = {
  normal: currentUser('normal-1', 'normaluser', 'NORMAL_USER'),
  staff: currentUser('staff-1', 'staffuser', 'STAFF'),
  supervisor: currentUser('supervisor-1', 'supervisor', 'SUPERVISOR')
};

const documents = [
  {
    documentId: 'doc-1',
    title: 'Policy PDF',
    latestVersionNumber: 2,
    latestVersionId: 'ver-1',
    thumbnailUrl: '/api/documents/doc-1/versions/ver-1/thumbnail',
    uploadedByUserId: 'supervisor-1',
    uploadedByName: 'supervisor',
    createdAt: '2026-06-01T10:00:00',
    updatedAt: '2026-06-01T11:00:00'
  },
  {
    documentId: 'doc-2',
    title: 'Finance Workbook',
    latestVersionNumber: 1,
    latestVersionId: 'ver-xlsx',
    thumbnailUrl: '/api/documents/doc-2/versions/ver-xlsx/thumbnail',
    uploadedByUserId: 'normal-1',
    uploadedByName: 'normaluser',
    createdAt: '2026-06-01T12:00:00',
    updatedAt: '2026-06-01T12:00:00'
  }
];

const documentDetails = {
  documentId: 'doc-1',
  title: 'Policy PDF',
  latestVersionNumber: 2,
  uploadedByUserId: 'supervisor-1',
  uploadedByName: 'supervisor',
  assignments: [],
  versions: [
    version('ver-1', 1, 'policy.pdf', 'application/pdf'),
    version('ver-xlsx', 2, 'finance.xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')
  ]
};

const annotations = [
  {
    annotationId: 'ann-1',
    documentId: 'doc-1',
    versionId: 'ver-1',
    pageNumber: 1,
    annotationType: 'COMMENT',
    xPercent: 20,
    yPercent: 25,
    widthPercent: 5,
    heightPercent: 5,
    pixelX: 80,
    pixelY: 100,
    pixelWidth: 24,
    pixelHeight: 24,
    pageRenderWidth: 400,
    pageRenderHeight: 500,
    color: '#2563EB',
    strokeWidth: 2,
    selectedText: null,
    drawingData: null,
    createdByUserId: 'staff-1',
    createdByName: 'staffuser',
    createdAt: '2026-06-01T13:00:00',
    updatedAt: '2026-06-01T13:00:00',
    comments: [
      {
        commentId: 'comment-1',
        annotationId: 'ann-1',
        commentText: 'Please review @normaluser',
        createdByUserId: 'staff-1',
        createdByName: 'staffuser',
        createdAt: '2026-06-01T13:00:00',
        updatedAt: '2026-06-01T13:00:00'
      }
    ]
  }
];

test.beforeEach(async ({ page }) => {
  await setupApiMocks(page, users.supervisor);
});

test('login page screenshot', async ({ page }) => {
  await page.goto('/login');
  await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible();
  await screenshot(page, 'login-page.png');
});

test('document list screenshot', async ({ page }) => {
  await authenticated(page, users.supervisor);
  await page.goto('/documents');
  await expect(page.getByRole('link', { name: 'Policy PDF', exact: true })).toBeVisible();
  await screenshot(page, 'document-list.png');
});

test('upload document flow screenshot', async ({ page }) => {
  await authenticated(page, users.supervisor);
  await page.goto('/documents');
  await page.getByRole('button', { name: 'Upload Document' }).click();
  await page.locator('input[type="file"]').setInputFiles({
    name: 'playwright-upload.pdf',
    mimeType: 'application/pdf',
    buffer: Buffer.from(pdfFixture())
  });
  await page.getByRole('button', { name: /^Upload$/ }).click();
  await expect(page.locator('.upload-modal')).toHaveCount(0);
  await screenshot(page, 'upload-success.png');
});

test('PDF viewer screenshot', async ({ page }) => {
  await authenticated(page, users.supervisor);
  await page.goto('/viewer/doc-1/ver-1');
  await expect(page.getByText('policy.pdf')).toBeVisible();
  await expect(page.locator('app-comment-panel')).toBeVisible();
  await screenshot(page, 'pdf-viewer.png');
});

test('annotation and comment screenshot', async ({ page }) => {
  await authenticated(page, users.supervisor);
  await page.goto('/viewer/doc-1/ver-1');
  await expect(page.getByText('Please review @normaluser')).toBeVisible();
  await screenshot(page, 'annotation-comment.png');
});

test('mention autocomplete screenshot', async ({ page }) => {
  await authenticated(page, users.supervisor);
  await page.goto('/viewer/doc-1/ver-1');
  const replyBox = page.locator('.reply-box textarea').first();
  await replyBox.fill('@norm');
  await expect(page.locator('.reply-mentions button', { hasText: 'normaluser' })).toBeVisible();
  await screenshot(page, 'mention-autocomplete.png');
});

test('normal user read-only viewer screenshot', async ({ page }) => {
  await setupApiMocks(page, users.normal);
  await authenticated(page, users.normal);
  await page.goto('/viewer/doc-1/ver-1');
  await expect(page.locator('app-annotation-toolbar')).toHaveCount(0);
  await expect(page.getByText('Please review @normaluser')).toBeVisible();
  await screenshot(page, 'normal-user-readonly-view.png');
});

test('staff unassigned document request screenshot', async ({ page }) => {
  await setupApiMocks(page, users.staff);
  await authenticated(page, users.staff);
  await page.goto('/documents/doc-1');
  await page.getByRole('button', { name: 'Request to Add to My Bucket' }).click();
  await expect(page.getByText('Request Pending')).toBeVisible();
  await screenshot(page, 'staff-request-document.png');
});

test('supervisor assignment approval screenshot', async ({ page }) => {
  await authenticated(page, users.supervisor);
  await page.goto('/assignment-requests');
  await expect(page.getByText('Policy PDF')).toBeVisible();
  await screenshot(page, 'supervisor-approval.png');
});

test('compare mode screenshot', async ({ page }) => {
  await authenticated(page, users.supervisor);
  await page.goto('/compare?leftDocumentId=doc-1&leftVersionId=ver-1&rightDocumentId=doc-1&rightVersionId=ver-1');
  await expect(page.locator('label[for="left-document"]')).toBeVisible();
  await screenshot(page, 'compare-mode.png');
});

test('Excel viewer screenshot', async ({ page }) => {
  await authenticated(page, users.supervisor);
  await page.goto('/excel-viewer/doc-1/ver-xlsx');
  await expect(page.getByRole('button', { name: 'Finance' })).toBeVisible();
  await expect(page.getByText('Revenue')).toBeVisible();
  await screenshot(page, 'excel-viewer.png');
});

async function authenticated(page: Page, user: ReturnType<typeof currentUser>) {
  await setupApiMocks(page, user);
  await page.addInitScript(([authToken]) => {
    window.localStorage.setItem('docuvra.auth.token', authToken);
  }, [token]);
}

async function setupApiMocks(page: Page, user: ReturnType<typeof currentUser>) {
  await page.route('**/api/auth/me', route => route.fulfill({ json: user }));
  await page.route('**/api/auth/login', route => route.fulfill({ json: { token, user } }));
  await page.route('**/api/users/mentionable', route => route.fulfill({ json: Object.values(users) }));
  await page.route('**/api/users', route => route.fulfill({ json: [users.staff, users.supervisor] }));
  await page.route('**/api/notifications/summary', route => route.fulfill({ json: { unreadCount: 1 } }));
  await page.route('**/api/notifications', route => route.fulfill({ json: [] }));
  await page.route('**/api/documents/upload', route => route.fulfill({
    status: 201,
    json: {
      documentId: 'doc-upload',
      versionId: 'ver-upload',
      versionNumber: 1,
      fileName: 'playwright-upload.pdf',
      mimeType: 'application/pdf',
      fileSize: 128,
      status: 'READY'
    }
  }));
  await page.route('**/api/documents', route => route.fulfill({ json: documents }));
  await page.route('**/api/documents/doc-1', route => route.fulfill({
    json: {
      ...documentDetails,
      assignments: user.role === 'STAFF' ? [] : documentDetails.assignments
    }
  }));
  await page.route('**/api/documents/doc-1/assignment-requests', route => route.fulfill({
    status: 201,
    json: {
      requestId: 'request-1',
      documentId: 'doc-1',
      documentTitle: 'Policy PDF',
      requestedByUserId: 'staff-1',
      requestedByUsername: 'staffuser',
      status: 'PENDING',
      requestedAt: '2026-06-01T14:00:00',
      reviewComment: null
    }
  }));
  await page.route('**/api/assignment-requests/pending', route => route.fulfill({ json: [{
    requestId: 'request-1',
    documentId: 'doc-1',
    documentTitle: 'Policy PDF',
    requestedByUserId: 'staff-1',
    requestedByUsername: 'staffuser',
    status: 'PENDING',
    requestedAt: '2026-06-01T14:00:00',
    reviewComment: null
  }] }));
  await page.route('**/api/documents/doc-1/versions/ver-1/annotations', route => {
    if (route.request().method() === 'POST') {
      return route.fulfill({ status: 201, json: annotations[0] });
    }
    return route.fulfill({ json: annotations });
  });
  await page.route('**/api/annotations/ann-1/comments', route => route.fulfill({ status: 201, json: annotations[0].comments[0] }));
  await page.route('**/api/documents/doc-1/versions/ver-1/view**', route => route.fulfill({
    status: 200,
    contentType: 'application/pdf',
    body: Buffer.from(pdfFixture())
  }));
  await page.route('**/api/documents/doc-1/versions/ver-xlsx/excel/workbook', route => route.fulfill({ json: {
    documentId: 'doc-1',
    versionId: 'ver-xlsx',
    fileName: 'finance.xlsx',
    sheets: [{ sheetIndex: 0, sheetName: 'Finance', rowCount: 2, columnCount: 2 }]
  }}));
  await page.route('**/api/documents/doc-1/versions/ver-xlsx/excel/sheets/0**', route => route.fulfill({ json: {
    sheetIndex: 0,
    sheetName: 'Finance',
    startRow: 0,
    rowCount: 2,
    totalRows: 2,
    columns: [{ index: 0, name: 'A' }, { index: 1, name: 'B' }],
    rows: [
      { rowIndex: 0, cells: [{ rowIndex: 0, columnIndex: 0, cellRef: 'A1', value: 'Revenue', displayValue: 'Revenue', cellType: 'STRING' }, { rowIndex: 0, columnIndex: 1, cellRef: 'B1', value: '1000', displayValue: '1000', cellType: 'NUMERIC' }] },
      { rowIndex: 1, cells: [{ rowIndex: 1, columnIndex: 0, cellRef: 'A2', value: 'Cost', displayValue: 'Cost', cellType: 'STRING' }, { rowIndex: 1, columnIndex: 1, cellRef: 'B2', value: '500', displayValue: '500', cellType: 'NUMERIC' }] }
    ]
  }}));
  await page.route('**/api/documents/doc-1/versions/ver-xlsx/excel/sheets/0/comments', route => route.fulfill({ json: [] }));
  await page.route('**/api/documents/**/thumbnail**', route => route.fulfill({
    status: 200,
    contentType: 'image/png',
    body: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=', 'base64')
  }));
}

async function screenshot(page: Page, name: string) {
  await page.screenshot({ path: path.join(screenshotsDir, name), fullPage: false });
}

function currentUser(userId: string, username: string, role: 'NORMAL_USER' | 'STAFF' | 'SUPERVISOR') {
  return {
    userId,
    username,
    email: `${username}@docuvra.local`,
    mobile: '+1000000000',
    role,
    forcePasswordChange: false,
    loginEnabled: true
  };
}

function version(versionId: string, versionNumber: number, fileName: string, mimeType: string) {
  return {
    versionId,
    versionNumber,
    fileName,
    mimeType,
    fileSize: 1024,
    pageCount: 1,
    status: 'READY',
    thumbnailUrl: `/api/documents/doc-1/versions/${versionId}/thumbnail`,
    uploadedAt: '2026-06-01T10:00:00'
  };
}

function pdfFixture() {
  return `%PDF-1.4
1 0 obj
<< /Type /Catalog /Pages 2 0 R >>
endobj
2 0 obj
<< /Type /Pages /Kids [3 0 R] /Count 1 >>
endobj
3 0 obj
<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 240] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>
endobj
4 0 obj
<< /Length 44 >>
stream
BT /F1 14 Tf 40 140 Td (Docuvra UI test) Tj ET
endstream
endobj
5 0 obj
<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>
endobj
xref
0 6
0000000000 65535 f
0000000009 00000 n
0000000058 00000 n
0000000115 00000 n
0000000241 00000 n
0000000335 00000 n
trailer
<< /Root 1 0 R /Size 6 >>
startxref
405
%%EOF`;
}
