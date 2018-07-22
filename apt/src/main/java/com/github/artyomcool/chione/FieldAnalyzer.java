package com.github.artyomcool.chione;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.*;

class FieldAnalyzer {

    private static final String SETTER_PREFIX = "set";
    private static final String GETTER_PREFIX = "get";
    private static final String GETTER_PREFIX_FOR_BOOLEAN = "is";

    private final Map<String, ExecutableElement> getters = new HashMap<>();
    private final Map<String, ExecutableElement> setters = new HashMap<>();
    private final List<MethodDescriptor> methods = new ArrayList<>();

    private FieldAnalyzer() {
    }

    static List<MethodDescriptor> analyze(TypeElement element) {
        FieldAnalyzer analyzer = new FieldAnalyzer();
        analyzer.collect(element);
        analyzer.sameNameStep();
        analyzer.beanStyleStep();
        analyzer.verifyNothingLeftStep();
        return analyzer.methods;
    }

    private void collect(TypeElement element) {
        VoidVisitor visitor = new VoidVisitor() {
            @Override
            protected void visitExecutable(ExecutableElement e) {
                if (e.isDefault()) {
                    return;
                }

                switch (e.getParameters().size()) {
                    case 0:
                        //TODO getterVerifyVisitor.visit(e);
                        getters.put(e.getSimpleName().toString(), e);
                        return;

                    case 1:
                        //TODO settersVerifyVisitor.visit(e);
                        setters.put(e.getSimpleName().toString(), e);
                        return;
                }

                throw new IllegalArgumentException("Unsupported method type (getter or setter expected): " + e);
            }
        };

        for (Element enclosed : element.getEnclosedElements()) {
            visitor.visit(enclosed);
        }
    }

    private void sameNameStep() {
        Iterator<Map.Entry<String, ExecutableElement>> iterator = getters.entrySet().iterator();
        while (iterator.hasNext()) {

            Map.Entry<String, ExecutableElement> nextGetterEntry = iterator.next();
            String getterName = nextGetterEntry.getKey();
            ExecutableElement nextGetter = nextGetterEntry.getValue();

            ExecutableElement nextSetter = setters.remove(getterName);

            if (nextSetter != null) {
                MethodDescriptor relatedMethods = new MethodDescriptor(getterName, nextGetter, nextSetter);
                methods.add(relatedMethods);
                iterator.remove();
            }
        }
    }

    private void beanStyleStep() {
        Iterator<Map.Entry<String, ExecutableElement>> iterator = setters.entrySet().iterator();
        while (iterator.hasNext()) {

            Map.Entry<String, ExecutableElement> nextSetterEntry = iterator.next();

            String setterName = nextSetterEntry.getKey();
            ExecutableElement nextSetter = nextSetterEntry.getValue();

            int setterPrefixLength = SETTER_PREFIX.length();
            if (setterName.length() <= setterPrefixLength) {
                continue;
            }

            if (!setterName.startsWith(SETTER_PREFIX)) {
                continue;
            }

            char firstLetter = setterName.charAt(setterPrefixLength);
            if (!Character.isUpperCase(firstLetter)) {
                continue;
            }

            String fieldNameCapitalized = setterName.substring(setterPrefixLength);

            ExecutableElement nextGetter = getters.remove(GETTER_PREFIX + fieldNameCapitalized);
            if (nextGetter == null) {
                nextGetter = getters.remove(GETTER_PREFIX_FOR_BOOLEAN + fieldNameCapitalized);
            }

            if (nextGetter != null) {
                String name = Character.toLowerCase(firstLetter) + fieldNameCapitalized.substring(1);

                MethodDescriptor relatedMethods = new MethodDescriptor(name, nextSetter, nextSetter);
                methods.add(relatedMethods);
                iterator.remove();
            }
        }
    }

    private void verifyNothingLeftStep() {
        //TODO
    }

}
