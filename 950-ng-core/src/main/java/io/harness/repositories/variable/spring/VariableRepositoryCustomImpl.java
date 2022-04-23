package io.harness.repositories.variable.spring;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.variable.entity.Variable;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

import java.util.List;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
@AllArgsConstructor(onConstructor = @__({ @Inject}))
@Singleton
public class VariableRepositoryCustomImpl implements VariableRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Variable> findAll(Criteria criteria, Pageable pageable) {
        Query query = new Query(criteria).with(pageable);
        List<Variable> variables = mongoTemplate.find(query, Variable.class);
        return PageableExecutionUtils.getPage(
                variables, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Variable.class));
    }

}
