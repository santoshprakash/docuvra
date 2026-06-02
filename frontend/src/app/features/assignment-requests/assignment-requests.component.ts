import { DatePipe } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AssignmentRequestResponse } from '../../core/models/assignment-request.model';
import { AssignmentRequestService } from '../../core/services/assignment-request.service';

@Component({
  selector: 'app-assignment-requests',
  standalone: true,
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './assignment-requests.component.html',
  styleUrl: './assignment-requests.component.scss'
})
export class AssignmentRequestsComponent implements OnInit {
  protected requests: AssignmentRequestResponse[] = [];
  protected rejectComments: Record<string, string> = {};
  protected errorMessage = '';
  protected isLoading = false;

  constructor(private readonly assignmentRequestService: AssignmentRequestService) {
  }

  ngOnInit(): void {
    this.loadRequests();
  }

  protected loadRequests(): void {
    this.isLoading = true;
    this.assignmentRequestService.pendingRequests().subscribe({
      next: requests => {
        this.requests = requests;
        this.isLoading = false;
      },
      error: error => {
        this.errorMessage = error?.error?.message ?? 'Unable to load assignment requests.';
        this.isLoading = false;
      }
    });
  }

  protected approve(request: AssignmentRequestResponse): void {
    this.assignmentRequestService.approve(request.requestId).subscribe({
      next: () => this.requests = this.requests.filter(item => item.requestId !== request.requestId),
      error: error => this.errorMessage = error?.error?.message ?? 'Unable to approve request.'
    });
  }

  protected reject(request: AssignmentRequestResponse): void {
    this.assignmentRequestService.reject(request.requestId, this.rejectComments[request.requestId] ?? '').subscribe({
      next: () => this.requests = this.requests.filter(item => item.requestId !== request.requestId),
      error: error => this.errorMessage = error?.error?.message ?? 'Unable to reject request.'
    });
  }
}
