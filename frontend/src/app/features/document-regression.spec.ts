import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpEventType } from '@angular/common/http';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { DocumentDetailsComponent } from './document-details/document-details.component';
import { DocumentListComponent } from './document-list/document-list.component';
import { DocumentUploadComponent } from './document-upload/document-upload.component';
import { ExcelViewerComponent } from './excel-viewer/excel-viewer.component';
import { AuthService } from '../core/services/auth.service';
import { DocumentService } from '../core/services/document.service';
import { ExcelPreviewService } from '../core/services/excel-preview.service';
import { authServiceFor, documentDetails, documentList, staffUser, supervisorUser, uploadResponse } from '../testing/regression-test-data';

describe('document-regression', () => {
  it('renders the document list with thumbnails and document links', async () => {
    const documentService = {
      listDocuments: jasmine.createSpy('listDocuments').and.returnValue(of(documentList)),
      getApiAssetUrl: (path: string | null) => `asset:${path}`
    };
    await TestBed.configureTestingModule({
      imports: [DocumentListComponent],
      providers: [
        provideRouter([]),
        { provide: DocumentService, useValue: documentService }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(DocumentListComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Policy PDF');
    expect(fixture.nativeElement.querySelector('img')?.getAttribute('src')).toContain('asset:/api/documents');
    expect(fixture.nativeElement.querySelector('.document-name-link')).toBeTruthy();
  });

  it('validates upload component and emits uploaded document response', async () => {
    const documentService = {
      uploadFreshDocumentWithProgress: jasmine.createSpy('uploadFreshDocumentWithProgress').and.returnValue(of(
        { type: HttpEventType.UploadProgress, loaded: 50, total: 100 },
        { type: HttpEventType.Response, body: uploadResponse }
      ))
    };
    await TestBed.configureTestingModule({
      imports: [DocumentUploadComponent],
      providers: [{ provide: DocumentService, useValue: documentService }]
    }).compileComponents();

    const fixture = TestBed.createComponent(DocumentUploadComponent);
    const component = fixture.componentInstance as any;
    const emitted: unknown[] = [];
    fixture.componentInstance.uploaded.subscribe(value => emitted.push(value));
    component.selectedFile = new File(['pdf'], 'sample.pdf', { type: 'application/pdf' });

    component.upload();
    fixture.detectChanges();

    expect(documentService.uploadFreshDocumentWithProgress).toHaveBeenCalled();
    expect(component.uploadProgress).toBe(100);
    expect(emitted[0]).toEqual(uploadResponse);
  });

  it('renders version list, thumbnails, PDF viewer route, and Excel viewer route', async () => {
    const documentService = {
      getDocumentDetails: jasmine.createSpy('getDocumentDetails').and.returnValue(of(documentDetails)),
      getApiAssetUrl: (path: string | null) => `asset:${path}`,
      getDownloadUrl: () => 'download-url'
    };
    await TestBed.configureTestingModule({
      imports: [DocumentDetailsComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: routeWith({ documentId: 'doc-1' }) },
        { provide: DocumentService, useValue: documentService },
        { provide: AuthService, useValue: authServiceFor(supervisorUser) }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(DocumentDetailsComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Version 2');
    expect(fixture.nativeElement.textContent).toContain('sheet.xlsx');
    expect((fixture.componentInstance as any).viewRoute(documentDetails.versions[0])).toEqual(['/viewer', 'doc-1', 'ver-1']);
    expect((fixture.componentInstance as any).viewRoute(documentDetails.versions[1])).toEqual(['/excel-viewer', 'doc-1', 'ver-2']);
    expect(fixture.nativeElement.querySelectorAll('img').length).toBeGreaterThan(0);
  });

  it('loads the Excel viewer workbook and grid', async () => {
    const excelPreviewService = {
      getWorkbook: jasmine.createSpy('getWorkbook').and.returnValue(of({
        documentId: 'doc-1',
        versionId: 'ver-2',
        fileName: 'sheet.xlsx',
        sheets: [{ sheetIndex: 0, sheetName: 'Sheet1', rowCount: 1, columnCount: 1 }]
      })),
      getSheet: jasmine.createSpy('getSheet').and.returnValue(of({
        sheetIndex: 0,
        sheetName: 'Sheet1',
        startRow: 0,
        rowCount: 1,
        totalRows: 1,
        columns: [{ columnIndex: 0, columnName: 'A' }],
        rows: [{ rowIndex: 0, cells: [{ rowIndex: 0, columnIndex: 0, cellRef: 'A1', value: 'Hello', displayValue: 'Hello' }] }]
      })),
      getComments: jasmine.createSpy('getComments').and.returnValue(of([]))
    };
    await TestBed.configureTestingModule({
      imports: [ExcelViewerComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: routeWith({ documentId: 'doc-1', versionId: 'ver-2' }) },
        { provide: ExcelPreviewService, useValue: excelPreviewService },
        { provide: DocumentService, useValue: { getDownloadUrl: () => 'download-url' } }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(ExcelViewerComponent);
    fixture.detectChanges();

    expect(excelPreviewService.getWorkbook).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Sheet1');
    expect(fixture.nativeElement.textContent).toContain('Hello');
  });

  it('shows staff request bucket button for unassigned documents', async () => {
    const documentService = {
      getDocumentDetails: jasmine.createSpy('getDocumentDetails').and.returnValue(of(documentDetails)),
      getApiAssetUrl: (path: string | null) => path ?? '',
      getDownloadUrl: () => 'download-url',
      requestAssignment: jasmine.createSpy('requestAssignment').and.returnValue(of({ id: 'req-1', status: 'PENDING' }))
    };
    await TestBed.configureTestingModule({
      imports: [DocumentDetailsComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: routeWith({ documentId: 'doc-1' }) },
        { provide: DocumentService, useValue: documentService },
        { provide: AuthService, useValue: authServiceFor(staffUser) }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(DocumentDetailsComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Request to Add to My Bucket');
  });
});

function routeWith(params: Record<string, string>) {
  return {
    snapshot: {
      paramMap: {
        get: (key: string) => params[key] ?? null
      }
    }
  };
}
