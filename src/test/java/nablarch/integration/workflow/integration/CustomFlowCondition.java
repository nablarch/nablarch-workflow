package nablarch.integration.workflow.integration;

import java.util.Map;

import nablarch.integration.workflow.condition.FlowProceedCondition;
import nablarch.integration.workflow.definition.SequenceFlow;

/**
 * コメント。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class CustomFlowCondition implements FlowProceedCondition {

    private static final int NORMAL_USER_LIMIT = 10000;

    @Override
    public boolean isMatch(String instanceId, Map<String, ?> param, SequenceFlow sequenceFlow) {
        TestEntity entity = (TestEntity) param.get("entity");

        if ("t02".equals(sequenceFlow.getTargetFlowNodeId())) {
            if (entity.userType == TestEntity.UserType.GOLD) {
                return true;
            } else {
                return entity.amount < NORMAL_USER_LIMIT;
            }
        } else {
            if (entity.userType == TestEntity.UserType.NORMAL && entity.amount >= NORMAL_USER_LIMIT) {
                return true;
            }
            return false;
        }
    }

    public static class TestEntity {
        public final long amount;
        public final UserType userType;

        public TestEntity(long amount, UserType userType) {
            this.amount = amount;
            this.userType = userType;
        }

        public enum UserType {
            NORMAL,
            GOLD
        }
    }
}
