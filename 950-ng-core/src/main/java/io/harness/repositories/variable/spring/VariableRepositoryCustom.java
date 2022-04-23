package io.harness.repositories.variable.spring;


import io.harness.annotations.dev.OwnedBy;

import io.harness.ng.core.variable.entity.Variable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
public interface VariableRepositoryCustom {
    Page<Variable> findAll(Criteria criteria, Pageable pageable);
}
