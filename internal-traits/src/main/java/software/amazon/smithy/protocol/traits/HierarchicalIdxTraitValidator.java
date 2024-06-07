/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.protocol.traits;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * A validator for the hierarchicalIdx trait that verifies the hierarchy is not cyclic and
 * that all members in the hierarchy are redeclared.
 */
public final class HierarchicalIdxTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        // This doesn't use TopologicalShapeSort because @hierarchicalIdx explicitly represents a tree
        // of shapes and not a graph. In particular, while building the reverse dependency lookup,
        // we need to fail if any shape has multiple incoming edges, which is completely allowed
        // by TopologicalShapeSort.

        // A map of shapes to their parents
        Map<StructureShape, StructureShape> reverseDependencyMap = new HashMap<>();
        // A map of shapes to their subshapes
        Map<StructureShape, Set<StructureShape>> forwardDependencyMap = new HashMap<>();
        for (StructureShape current : sorted(model.getStructureShapesWithTrait(HierarchicalIdxTrait.class))) {
            Set<StructureShape> children = current.expectTrait(HierarchicalIdxTrait.class)
                .getValues()
                .stream()
                .map(id -> model.expectShape(ShapeId.from(id), StructureShape.class))
                .collect(Collectors.toCollection(LinkedHashSet::new));
            forwardDependencyMap.put(current, children);
            for (StructureShape child : children) {
                if (reverseDependencyMap.containsKey(child)) {
                    events.add(
                        error(
                            current,
                            current.expectTrait(HierarchicalIdxTrait.class),
                            String.format(
                                "Shape \"%s\" is declared in multiple @hierarchicalIdx traits, which is " +
                                    "not allowed: \"%s\", \"%s\"",
                                child.toShapeId(),
                                reverseDependencyMap.get(child).toShapeId(),
                                current.toShapeId()
                            )
                        )
                    );
                } else {
                    reverseDependencyMap.put(child, current);
                }
            }
        }

        // Queue for topological ordering
        Queue<StructureShape> toProcess = new LinkedList<>();
        for (StructureShape s : forwardDependencyMap.keySet()) {
            if (!reverseDependencyMap.containsKey(s)) {
                toProcess.add(s);
            }
        }

        // This is the main loop of the topological sort. Since each structure must strictly
        // include its parents members and there is no multiple inheritance this DAG is not
        // very complicated and topological sort doesn't do a lot of heavy lifting here.
        while (!toProcess.isEmpty()) {
            StructureShape current = toProcess.poll();
            StructureShape parent = reverseDependencyMap.remove(current);

            if (parent != null) {
                for (final MemberShape parentMember : parent.members()) {
                    Optional<MemberShape> currentMember = current.getMember(parentMember.getMemberName());
                    if (!currentMember.isPresent()) {
                        events.add(
                            error(
                                current,
                                current,
                                String.format(
                                    "Shape \"%s\" is hierarchical child of \"%s\""
                                        + " but does not define member \"%s\"",
                                    current.toShapeId(),
                                    parent.toShapeId(),
                                    parentMember.getMemberName()
                                )
                            )
                        );

                    } else {
                        if (!currentMember.get().getTarget().equals(parentMember.getTarget())) {
                            events.add(
                                error(
                                    current,
                                    currentMember.get(),
                                    String.format(
                                        "Shape \"%s\" is hierarchical child of \"%s\""
                                            + " but member \"%s\" targets shape \"%s\" "
                                            + "while member in \"%s\" targets shape \"%s\"",
                                        current.toShapeId(),
                                        parent.toShapeId(),
                                        parentMember.getMemberName(),
                                        currentMember.get().getTarget(),
                                        parent.toShapeId(),
                                        parentMember.getTarget()
                                    )
                                )
                            );
                        }
                        validateMemberTraits(events, current, parent, parentMember, currentMember.get());
                    }
                }
            }

            if (forwardDependencyMap.containsKey(current)) {
                toProcess.addAll(forwardDependencyMap.remove(current));
            }
        }

        if (!reverseDependencyMap.isEmpty()) {
            final List<StructureShape> sortedSuspects = reverseDependencyMap.keySet()
                .stream()
                .sorted(Comparator.comparing(Shape::toShapeId))
                .collect(Collectors.toList());
            events.add(
                error(
                    sortedSuspects.get(0),
                    String.format(
                        "Cyclic `hierarchicalIdx` relationship found among shapes: %s",
                        sortedSuspects.stream()
                            .map(s -> s.toShapeId().toString())
                            .collect(Collectors.joining(", "))
                    )
                )
            );
        }

        return events;
    }

    private Iterable<? extends StructureShape> sorted(Set<StructureShape> structureShapesWithTrait) {
        return structureShapesWithTrait.stream()
            .sorted(Comparator.comparing(Shape::toShapeId))
            .collect(Collectors.toList());
    }

    private void validateMemberTraits(
        List<ValidationEvent> events,
        StructureShape current,
        StructureShape parent,
        MemberShape parentMember,
        MemberShape currentMember
    ) {
        final Map<ShapeId, Trait> currentTraits = new HashMap<>(currentMember.getAllTraits());
        for (Map.Entry<ShapeId, Trait> e : parentMember.getAllTraits().entrySet()) {
            final ShapeId traitId = e.getKey();
            final Trait currentTrait = currentTraits.remove(traitId);
            if (currentTrait == null) {
                events.add(
                    error(
                        current,
                        current,
                        String.format(
                            "Shape \"%s\" is hierarchical child of \"%s\" but member \"%s\" is missing trait "
                                + "\"%s\" defined by its parent",
                            current.toShapeId(),
                            parent.toShapeId(),
                            parentMember.getMemberName(),
                            traitId
                        )
                    )
                );
            } else if (!currentTrait.toNode().equals(e.getValue().toNode())) {
                events.add(
                    error(
                        current,
                        currentTrait,
                        String.format(
                            "Shape \"%s\" is hierarchical child of \"%s\" but trait \"%s\""
                                + " on member \"%s\" is "
                                + "defined differently than the same trait on the parent's member",
                            current.toShapeId(),
                            parent.toShapeId(),
                            traitId,
                            parentMember.getMemberName()
                        )
                    )
                );
            }
        }
        if (!currentTraits.isEmpty()) {
            events.add(
                error(
                    current,
                    currentTraits.values().iterator().next(),
                    String.format(
                        "Shape \"%s\" is hierarchical child of \"%s\" but member \"%s\""
                            + " defines additional traits not defined on its parent: %s",
                        current.toShapeId(),
                        parent.toShapeId(),
                        parentMember.getMemberName(),
                        currentTraits.keySet()
                            .stream()
                            .map(ShapeId::toString)
                            .collect(Collectors.joining(", "))
                    )
                )
            );
        }
    }
}
