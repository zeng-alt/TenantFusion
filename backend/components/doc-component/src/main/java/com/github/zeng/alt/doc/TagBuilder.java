package com.github.zeng.alt.doc;

import io.swagger.v3.oas.models.tags.Tag;

import java.util.List;

public class TagBuilder {

    public List<Tag> build(List<TagProperties> list) {

        return list.stream()

                .map(t -> new Tag()
                        .name(t.getName())
                        .description(t.getDescription()))

                .toList();
    }

}