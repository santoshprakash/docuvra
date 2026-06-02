package com.docuvra.repository;

import com.docuvra.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    List<DocumentEntity> findAllByOrderByUpdatedAtDesc();

    List<DocumentEntity> findAllByUploadedByUserIdOrderByUpdatedAtDesc(UUID uploadedByUserId);

    @Query("""
            select distinct document
            from DocumentEntity document
            join DocumentAssignmentEntity assignment on assignment.document.id = document.id
            where assignment.assignedToUser.id = :userId
              and assignment.status = com.docuvra.enums.DocumentAssignmentStatus.ASSIGNED
            order by document.updatedAt desc
            """)
    List<DocumentEntity> findAssignedDocuments(@Param("userId") UUID userId);

    @Query("""
            select document
            from DocumentEntity document
            where not exists (
                select assignment.id
                from DocumentAssignmentEntity assignment
                where assignment.document.id = document.id
                  and assignment.status = com.docuvra.enums.DocumentAssignmentStatus.ASSIGNED
            )
            order by document.updatedAt desc
            """)
    List<DocumentEntity> findUnassignedDocuments();

    @Query("""
            select distinct document
            from DocumentEntity document
            left join DocumentAssignmentEntity assignment
                on assignment.document.id = document.id
                and assignment.status = com.docuvra.enums.DocumentAssignmentStatus.ASSIGNED
            where (assignment.assignedToUser.id = :userId)
               or assignment.id is null
            order by document.updatedAt desc
            """)
    List<DocumentEntity> findStaffVisibleDocuments(@Param("userId") UUID userId);
}
