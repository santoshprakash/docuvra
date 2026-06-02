import { Routes } from '@angular/router';
import { authGuard, passwordChangeGuard, supervisorGuard } from './core/services/auth.guard';
import { ChangePasswordComponent } from './features/change-password/change-password.component';
import { AssignmentRequestsComponent } from './features/assignment-requests/assignment-requests.component';
import { CompareComponent } from './features/compare/compare.component';
import { DocumentDetailsComponent } from './features/document-details/document-details.component';
import { DocumentListComponent } from './features/document-list/document-list.component';
import { ExcelViewerComponent } from './features/excel-viewer/excel-viewer.component';
import { LoginComponent } from './features/login/login.component';
import { UserManagementComponent } from './features/user-management/user-management.component';
import { ViewerComponent } from './features/viewer/viewer.component';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'documents'
  },
  {
    path: 'documents',
    component: DocumentListComponent,
    canActivate: [authGuard]
  },
  {
    path: 'documents/:documentId',
    component: DocumentDetailsComponent,
    canActivate: [authGuard]
  },
  {
    path: 'viewer/:documentId/:versionId',
    component: ViewerComponent,
    canActivate: [authGuard]
  },
  {
    path: 'excel-viewer/:documentId/:versionId',
    component: ExcelViewerComponent,
    canActivate: [authGuard]
  },
  {
    path: 'compare',
    component: CompareComponent,
    canActivate: [authGuard]
  },
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'change-password',
    component: ChangePasswordComponent,
    canActivate: [passwordChangeGuard]
  },
  {
    path: 'users',
    component: UserManagementComponent,
    canActivate: [supervisorGuard]
  },
  {
    path: 'assignment-requests',
    component: AssignmentRequestsComponent,
    canActivate: [supervisorGuard]
  }
];
