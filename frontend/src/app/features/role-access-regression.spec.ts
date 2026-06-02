import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AssignmentRequestsComponent } from './assignment-requests/assignment-requests.component';
import { DocumentDetailsComponent } from './document-details/document-details.component';
import { CommentPanelComponent } from './viewer/comment-panel/comment-panel.component';
import { ViewerComponent } from './viewer/viewer.component';
import { AnnotationService } from '../core/services/annotation.service';
import { AssignmentRequestService } from '../core/services/assignment-request.service';
import { AuthService } from '../core/services/auth.service';
import { DocumentService } from '../core/services/document.service';
import { annotation, authServiceFor, documentDetails, normalUser, staffUser, supervisorUser } from '../testing/regression-test-data';

describe('role-access-regression', () => {
  it('hides annotation toolbar and delete controls for NORMAL_USER while showing mentioned thread', async () => {
    const viewer = await createViewer(normalUser);
    expect(viewer.nativeElement.querySelector('app-annotation-toolbar')).toBeFalsy();

    await TestBed.resetTestingModule().configureTestingModule({ imports: [CommentPanelComponent] }).compileComponents();
    const comments = TestBed.createComponent(CommentPanelComponent);
    comments.componentRef.setInput('annotations', [annotation]);
    comments.componentRef.setInput('canDelete', false);
    comments.componentRef.setInput('canReply', true);
    comments.detectChanges();

    expect(comments.nativeElement.textContent).toContain('Mentioned comment');
    expect(comments.nativeElement.querySelector('.comment-delete')).toBeFalsy();
  });

  it('shows annotation toolbar for STAFF and request bucket button for unassigned documents', async () => {
    const viewer = await createViewer(staffUser);
    expect(viewer.nativeElement.querySelector('app-annotation-toolbar')).toBeTruthy();

    await TestBed.resetTestingModule().configureTestingModule({
      imports: [DocumentDetailsComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: routeWith({ documentId: 'doc-1' }) },
        { provide: DocumentService, useValue: detailsDocumentService() },
        { provide: AuthService, useValue: authServiceFor(staffUser) }
      ]
    }).compileComponents();
    const details = TestBed.createComponent(DocumentDetailsComponent);
    details.detectChanges();

    expect(details.nativeElement.textContent).toContain('Request to Add to My Bucket');
  });

  it('shows supervisor assignment controls and pending requests', async () => {
    await TestBed.configureTestingModule({
      imports: [DocumentDetailsComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: routeWith({ documentId: 'doc-1' }) },
        { provide: DocumentService, useValue: detailsDocumentService() },
        { provide: AuthService, useValue: authServiceFor(supervisorUser) }
      ]
    }).compileComponents();
    const details = TestBed.createComponent(DocumentDetailsComponent);
    details.detectChanges();

    expect(details.nativeElement.textContent).toContain('Staff assignments');
    expect(details.nativeElement.textContent).toContain('Assign');

    await TestBed.resetTestingModule().configureTestingModule({
      imports: [AssignmentRequestsComponent],
      providers: [
        provideRouter([]),
        {
          provide: AssignmentRequestService,
          useValue: {
            pendingRequests: () => of([{
              requestId: 'request-1',
              documentId: 'doc-1',
              documentTitle: 'Policy PDF',
              requestedByUserId: staffUser.userId,
              requestedByUsername: staffUser.username,
              status: 'PENDING',
              requestedAt: '2026-06-01T10:00:00',
              reviewedByUsername: null,
              reviewedAt: null,
              reviewComment: null
            }]),
            approve: () => of({}),
            reject: () => of({})
          }
        }
      ]
    }).compileComponents();
    const requests = TestBed.createComponent(AssignmentRequestsComponent);
    requests.detectChanges();

    expect(requests.nativeElement.textContent).toContain('Assignment requests');
    expect(requests.nativeElement.textContent).toContain('Policy PDF');
  });
});

async function createViewer(currentUser: any) {
  await TestBed.configureTestingModule({
    imports: [ViewerComponent],
    providers: [
      provideRouter([]),
      { provide: ActivatedRoute, useValue: routeWith({ documentId: 'doc-1', versionId: 'ver-1' }) },
      { provide: DocumentService, useValue: { getDocumentDetails: () => of(documentDetails), getViewUrl: () => 'view-url', getDownloadUrl: () => 'download-url' } },
      { provide: AnnotationService, useValue: { listAnnotations: () => of([annotation]), updateAnnotation: () => of(annotation), createComment: () => of(annotation.comments[0]) } },
      { provide: AuthService, useValue: authServiceFor(currentUser) }
    ]
  }).compileComponents();
  const fixture = TestBed.createComponent(ViewerComponent);
  fixture.detectChanges();
  return fixture;
}

function detailsDocumentService() {
  return {
    getDocumentDetails: () => of(documentDetails),
    getApiAssetUrl: (path: string | null) => path ?? '',
    getDownloadUrl: () => 'download-url',
    requestAssignment: () => of({ requestId: 'request-1', status: 'PENDING' }),
    assignDocument: () => of({}),
    removeAssignment: () => of({})
  };
}

function routeWith(params: Record<string, string>) {
  return {
    snapshot: {
      paramMap: {
        get: (key: string) => params[key] ?? null
      }
    }
  };
}
