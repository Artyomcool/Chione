package com.github.artyomcool.chione;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.SimpleElementVisitor8;

@SuppressWarnings("WeakerAccess")
public class VoidVisitor extends SimpleElementVisitor8<Void, Void> {
    @Override
    public final Void visitVariable(VariableElement variableElement, Void aVoid) {
        visitVariable(variableElement);
        return null;
    }

    @Override
    protected final Void defaultAction(Element element, Void aVoid) {
        defaultAction(element);
        return null;
    }

    @Override
    public final Void visitPackage(PackageElement packageElement, Void aVoid) {
        visitPackage(packageElement);
        return null;
    }

    @Override
    public final Void visitType(TypeElement typeElement, Void aVoid) {
        visitType(typeElement);
        return null;
    }

    @Override
    public final Void visitExecutable(ExecutableElement executableElement, Void aVoid) {
        visitExecutable(executableElement);
        return null;
    }

    @Override
    public final Void visitTypeParameter(TypeParameterElement typeParameterElement, Void aVoid) {
        visitTypeParameter(typeParameterElement);
        return null;
    }

    @Override
    public final Void visitUnknown(Element element, Void aVoid) {
        visitUnknown(element);
        return null;
    }

    protected void visitVariable(VariableElement variableElement) {
        super.visitVariable(variableElement, null);
    }

    protected void defaultAction(Element element) {
        super.defaultAction(element, null);
    }

    protected void visitPackage(PackageElement packageElement) {
        super.visitPackage(packageElement, null);
    }

    protected void visitType(TypeElement type) {
        super.visitType(type, null);
    }

    protected void visitExecutable(ExecutableElement executableElement) {
        super.visitExecutable(executableElement, null);
    }

    protected void visitTypeParameter(TypeParameterElement typeParameterElement) {
        super.visitTypeParameter(typeParameterElement, null);
    }

    protected void visitUnknown(Element element) {
        super.visitUnknown(element, null);
    }

}
