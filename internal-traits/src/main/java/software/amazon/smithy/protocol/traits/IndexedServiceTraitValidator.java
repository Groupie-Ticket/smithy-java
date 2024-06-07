/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.protocol.traits;

import static java.util.stream.Collectors.toSet;

import java.util.*;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.*;
import software.amazon.smithy.model.traits.EventHeaderTrait;
import software.amazon.smithy.model.traits.EventPayloadTrait;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * A validator for indexed services. If a shape is in the closure of an indexed service,
 * its members must be indexed, the ordering starts at 1 and has no gaps.
 */
public final class IndexedServiceTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        Set<ShapeId> shapesOfInterest = new HashSet<>();
        Set<ServiceShape> services = model.getServiceShapesWithTrait(IndexedServiceTrait.class);
        for (ServiceShape service : services) {
            new Walker(model).iterateShapes(service).forEachRemaining(shape -> {
                if ((shape.isStructureShape() || shape.isUnionShape())
                    && !shape.hasTrait(MixinTrait.class)) {
                    shapesOfInterest.add(shape.getId());
                }
            });
        }

        Map<ShapeId, Set<String>> inheritedMembers = new HashMap<>();
        for (StructureShape parent : model.getStructureShapesWithTrait(HierarchicalIdxTrait.class)) {
            Set<String> parentDeclared = parent.members().stream().map(MemberShape::getMemberName).collect(toSet());
            for (String child : parent.expectTrait(HierarchicalIdxTrait.class).getValues()) {
                inheritedMembers.put(ShapeId.from(child), parentDeclared);
            }
        }

        for (ShapeId shapeId : shapesOfInterest) {
            validateMembers(model, model.expectShape(shapeId), inheritedMembers, events);
        }

        return events;
    }

    private void validateMembers(
        Model model,
        Shape container,
        Map<ShapeId, Set<String>> inheritedMembers,
        List<ValidationEvent> events
    ) {
        int lastIdx = 0;
        List<MemberShape> sorted = new ArrayList<>(container.members());

        Set<String> parentMembers = inheritedMembers.getOrDefault(container.getId(), Collections.emptySet());
        for (Iterator<MemberShape> iter = sorted.iterator(); iter.hasNext();) {
            MemberShape ms = iter.next();
            if (parentMembers.contains(ms.getMemberName())) {
                iter.remove();
            }
        }

        sorted.sort(Comparator.comparingInt(t -> t.getTrait(IdxTrait.class).map(IdxTrait::getValue).orElse(-1)));

        for (MemberShape ms : sorted) {
            if (!ms.hasTrait(IdxTrait.class)) {
                if (ms.hasTrait(EventHeaderTrait.class) || ms.hasTrait(EventPayloadTrait.class)) {
                    continue;
                }
                Shape untraitedTarget = model.expectShape(ms.getTarget());
                if ((untraitedTarget.isBlobShape() || untraitedTarget.isUnionShape())
                    && untraitedTarget.hasTrait(StreamingTrait.class)) {
                    continue;
                }

                events.add(
                    error(
                        ms,
                        ms.getSourceLocation(),
                        String.format(
                            "Structure \"%s\" is in the closure of an indexed service, but its member "
                                + "\"%s\" is missing an idx trait",
                            ms.getContainer(),
                            ms.toShapeId()
                        )
                    )
                );

                continue;
            }

            IdxTrait trait = ms.expectTrait(IdxTrait.class);
            if (trait.getValue() == lastIdx) {
                events.add(error(ms, trait, String.format("Duplicate idx value \"%d\"", lastIdx)));
            } else if (trait.getValue() != ++lastIdx) {
                if (trait.getValue() - 1 == lastIdx) {
                    events.add(
                        error(
                            ms,
                            trait,
                            String.format(
                                "idx must increase monotonically starting at 1, no members found for idx "
                                    + "%d",
                                lastIdx
                            )
                        )
                    );

                } else {
                    events.add(
                        error(
                            ms,
                            trait,
                            String.format(
                                "idx must increase monotonically starting at 1, no members found for idxs "
                                    + "between %d and %d, inclusive",
                                lastIdx,
                                trait.getValue() - 1
                            )
                        )
                    );

                }
                lastIdx = trait.getValue();
            }
        }
    }
}
