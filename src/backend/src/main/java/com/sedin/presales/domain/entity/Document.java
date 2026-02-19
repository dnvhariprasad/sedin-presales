package com.sedin.presales.domain.entity;

import com.sedin.presales.domain.enums.DocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "documents")
public class Document extends BaseEntity {

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "document_date")
    private LocalDate documentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DocumentStatus status;

    @Column(name = "rag_indexed")
    private Boolean ragIndexed;

    @Column(name = "current_version_number")
    private Integer currentVersionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_type_id")
    private DocumentType documentType;

    @OneToOne(mappedBy = "document", fetch = FetchType.LAZY)
    private DocumentMetadata documentMetadata;

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY)
    @OrderBy("versionNumber DESC")
    private List<DocumentVersion> versions = new ArrayList<>();

}
