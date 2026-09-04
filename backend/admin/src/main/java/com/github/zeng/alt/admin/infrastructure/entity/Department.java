package com.github.zeng.alt.admin.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.zeng.alt.api.base.BaseTreeEntity;
import com.github.zeng.alt.tenant.row.TenantBaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import com.github.zeng.alt.domain.validation.UniqueCheck;
import com.github.zeng.alt.rest.annotation.QueryField;
import com.github.zeng.alt.rest.annotation.QueryOrder;
import com.github.zeng.alt.rest.annotation.QueryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.proxy.HibernateProxy;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "main_department")
@SQLDelete(sql = "update main_department set is_deleted=true where dept_id=?")
@SQLRestriction("is_deleted=false")
@Getter
@Setter
public class Department extends TenantBaseEntity<Long> implements BaseTreeEntity<Department> {

    @Id
    @SnowflakeId
    private Long deptId;

    @QueryField(type = QueryType.LIKE)
    private String deptName;

    @QueryOrder(autoSort = true)
    private Integer deptSort;

    @QueryField
    @Column(name = "is_enabled")
    private Boolean enabled = true;

    private String remark;

    @Column(name = "is_deleted")
    private Boolean deleted = false;

    @Transient
    private Long parentId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Department parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @OrderBy("deptSort ASC")
    private List<Department> children = new LinkedList<>();

    public void addChild(Department child) {
        children.add(child);
        child.setParent(this);
    }

    public void removeChild(Department child) {
        children.remove(child);
        child.setParent(null);
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
        if (parentId != null) {
            this.parent = new Department();
            this.parent.setDeptId(parentId);
        } else {
            this.parent = null;
        }
    }

    @Override
    public Long getId() {
        return deptId;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy
            ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
            : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
            : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Department that = (Department) o;
        return getDeptId() != null && Objects.equals(getDeptId(), that.getDeptId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
            : getClass().hashCode();
    }
}
