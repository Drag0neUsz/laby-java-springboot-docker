package com.example.SpringBootApp.client.sync;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collapses the changelog into the minimal set of REST calls to send.
 *
 * Per (EntityType, entityId) group, in insertion order:
 *   CREATE + UPDATE  -> single CREATE (POST) with the latest payload
 *   CREATE + DELETE  -> nothing (cancelled out)
 *   UPDATE + UPDATE  -> single UPDATE (PUT) with the latest payload
 *   UPDATE + DELETE  -> single DELETE
 *
 * Order of distinct entity groups is preserved (first occurrence wins).
 * Every merge/cancellation is logged to stdout so the reviewer can see
 * exactly which queries were eliminated.
 */
@Component
public class SyncOptimizer {

    public List<ChangeRecord> optimize(List<ChangeRecord> input) {
        System.out.println();
        System.out.println("================ SYNC OPTIMIZATION ================");
        System.out.println("[OPTIMIZER] Queue BEFORE optimization (" + input.size() + " ops):");
        if (input.isEmpty()) {
            System.out.println("           (empty)");
        } else {
            for (ChangeRecord r : input) {
                System.out.println("           " + r);
            }
        }

        Map<String, ChangeRecord> collapsed = new LinkedHashMap<>();

        for (ChangeRecord next : input) {
            String key = key(next.getEntityType(), next.getEntityId());
            ChangeRecord existing = collapsed.get(key);

            if (existing == null) {
                collapsed.put(key, copyOf(next));
                continue;
            }

            ActionType prev = existing.getActionType();
            ActionType cur = next.getActionType();

            if (prev == ActionType.CREATE && cur == ActionType.UPDATE) {
                System.out.println("[OPTIMIZER] CREATE + UPDATE on " + next.getEntityType()
                        + "(id=" + next.getEntityId() + ") -> merged into single POST");
                existing.setPayload(next.getPayload());
                existing.setBusinessRule(next.getBusinessRule());
            } else if (prev == ActionType.CREATE && cur == ActionType.DELETE) {
                System.out.println("[OPTIMIZER] CREATE + DELETE on " + next.getEntityType()
                        + "(id=" + next.getEntityId() + ") -> cancelled out (nothing to send)");
                collapsed.remove(key);
            } else if (prev == ActionType.UPDATE && cur == ActionType.UPDATE) {
                System.out.println("[OPTIMIZER] UPDATE + UPDATE on " + next.getEntityType()
                        + "(id=" + next.getEntityId() + ") -> merged into single PUT");
                existing.setPayload(next.getPayload());
                existing.setBusinessRule(next.getBusinessRule());
            } else if (prev == ActionType.UPDATE && cur == ActionType.DELETE) {
                System.out.println("[OPTIMIZER] UPDATE + DELETE on " + next.getEntityType()
                        + "(id=" + next.getEntityId() + ") -> collapsed to single DELETE");
                existing.setActionType(ActionType.DELETE);
                existing.setPayload(null);
                existing.setBusinessRule(next.getBusinessRule());
            } else if (prev == ActionType.DELETE) {
                System.out.println("[OPTIMIZER] " + cur + " after DELETE on " + next.getEntityType()
                        + "(id=" + next.getEntityId() + ") -> ignored (entity already scheduled for deletion)");
            } else {
                collapsed.put(key, copyOf(next));
            }
        }

        List<ChangeRecord> result = new ArrayList<>(collapsed.values());

        System.out.println("[OPTIMIZER] Queue AFTER optimization  (" + result.size() + " ops):");
        if (result.isEmpty()) {
            System.out.println("           (empty - nothing to send)");
        } else {
            for (ChangeRecord r : result) {
                System.out.println("           " + r);
            }
        }
        int saved = input.size() - result.size();
        System.out.println("[OPTIMIZER] " + saved + " redundant request(s) eliminated.");
        System.out.println("====================================================");
        System.out.println();
        return result;
    }

    private static String key(EntityType type, Integer id) {
        return type + "#" + id;
    }

    private static ChangeRecord copyOf(ChangeRecord src) {
        ChangeRecord r = new ChangeRecord(src.getEntityType(), src.getActionType(), src.getEntityId(), src.getPayload());
        r.setBusinessRule(src.getBusinessRule());
        return r;
    }
}
