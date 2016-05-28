package com.tngtech.archunit.core;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.tngtech.archunit.core.HasOwner.IsOwnedByMethod;
import com.tngtech.archunit.core.JavaFieldAccess.AccessType;
import com.tngtech.archunit.core.testexamples.annotationfieldimport.ClassWithAnnotatedFields.FieldAnnotationWithEnumAndArrayValue;
import com.tngtech.archunit.core.testexamples.annotationfieldimport.ClassWithAnnotatedFields.FieldAnnotationWithIntValue;
import com.tngtech.archunit.core.testexamples.annotationfieldimport.ClassWithAnnotatedFields.FieldAnnotationWithStringValue;
import com.tngtech.archunit.core.testexamples.annotationmethodimport.ClassWithAnnotatedMethods;
import com.tngtech.archunit.core.testexamples.annotationmethodimport.ClassWithAnnotatedMethods.MethodAnnotationWithEnumAndArrayValue;
import com.tngtech.archunit.core.testexamples.annotationmethodimport.ClassWithAnnotatedMethods.MethodAnnotationWithIntValue;
import com.tngtech.archunit.core.testexamples.annotationmethodimport.ClassWithAnnotatedMethods.MethodAnnotationWithStringValue;
import com.tngtech.archunit.core.testexamples.callimport.CallsExternalMethod;
import com.tngtech.archunit.core.testexamples.callimport.CallsMethodReturningArray;
import com.tngtech.archunit.core.testexamples.callimport.CallsMethodReturningArray.SomeEnum;
import com.tngtech.archunit.core.testexamples.callimport.CallsOtherConstructor;
import com.tngtech.archunit.core.testexamples.callimport.CallsOtherMethod;
import com.tngtech.archunit.core.testexamples.callimport.CallsOwnConstructor;
import com.tngtech.archunit.core.testexamples.callimport.CallsOwnMethod;
import com.tngtech.archunit.core.testexamples.callimport.ExternalInterfaceMethodCall;
import com.tngtech.archunit.core.testexamples.callimport.ExternalOverriddenMethodCall;
import com.tngtech.archunit.core.testexamples.callimport.ExternalSubTypeConstructorCall;
import com.tngtech.archunit.core.testexamples.classhierarchyimport.BaseClass;
import com.tngtech.archunit.core.testexamples.classhierarchyimport.OtherSubClass;
import com.tngtech.archunit.core.testexamples.classhierarchyimport.SubClass;
import com.tngtech.archunit.core.testexamples.classhierarchyimport.SubSubClass;
import com.tngtech.archunit.core.testexamples.complexexternal.ChildClass;
import com.tngtech.archunit.core.testexamples.diamond.ClassCallingDiamond;
import com.tngtech.archunit.core.testexamples.diamond.ClassImplementingD;
import com.tngtech.archunit.core.testexamples.diamond.InterfaceD;
import com.tngtech.archunit.core.testexamples.fieldaccessimport.ExternalFieldAccess;
import com.tngtech.archunit.core.testexamples.fieldaccessimport.ExternalShadowedFieldAccess;
import com.tngtech.archunit.core.testexamples.fieldaccessimport.ForeignFieldAccess;
import com.tngtech.archunit.core.testexamples.fieldaccessimport.ForeignFieldAccessFromConstructor;
import com.tngtech.archunit.core.testexamples.fieldaccessimport.ForeignFieldAccessFromStaticInitializer;
import com.tngtech.archunit.core.testexamples.fieldaccessimport.ForeignStaticFieldAccess;
import com.tngtech.archunit.core.testexamples.fieldaccessimport.MultipleFieldAccessInSameMethod;
import com.tngtech.archunit.core.testexamples.fieldaccessimport.OwnFieldAccess;
import com.tngtech.archunit.core.testexamples.fieldaccessimport.OwnStaticFieldAccess;
import com.tngtech.archunit.core.testexamples.fieldimport.ClassWithIntAndObjectFields;
import com.tngtech.archunit.core.testexamples.hierarchicalfieldaccess.AccessToSuperAndSubClassField;
import com.tngtech.archunit.core.testexamples.hierarchicalfieldaccess.SubClassWithAccessedField;
import com.tngtech.archunit.core.testexamples.hierarchicalfieldaccess.SuperClassWithAccessedField;
import com.tngtech.archunit.core.testexamples.hierarchicalmethodcall.CallOfSuperAndSubClassMethod;
import com.tngtech.archunit.core.testexamples.hierarchicalmethodcall.SubClassWithCalledMethod;
import com.tngtech.archunit.core.testexamples.hierarchicalmethodcall.SuperClassWithCalledMethod;
import com.tngtech.archunit.core.testexamples.innerclassimport.CalledClass;
import com.tngtech.archunit.core.testexamples.innerclassimport.ClassWithInnerClass;
import com.tngtech.archunit.core.testexamples.integration.ClassA;
import com.tngtech.archunit.core.testexamples.integration.ClassBDependingOnClassA;
import com.tngtech.archunit.core.testexamples.integration.ClassCDependingOnClassB;
import com.tngtech.archunit.core.testexamples.integration.ClassD;
import com.tngtech.archunit.core.testexamples.integration.ClassXDependingOnClassesABCD;
import com.tngtech.archunit.core.testexamples.nestedimport.ClassWithNestedClass;
import com.tngtech.archunit.core.testexamples.simpleimport.ClassToImportOne;
import com.tngtech.archunit.core.testexamples.simpleimport.ClassToImportTwo;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Sets.newHashSet;
import static com.tngtech.archunit.core.ClassFileImporter.JAR_PROTOCOL;
import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.JavaFieldAccess.AccessType.GET;
import static com.tngtech.archunit.core.JavaFieldAccess.AccessType.SET;
import static com.tngtech.archunit.core.JavaModifier.FINAL;
import static com.tngtech.archunit.core.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.JavaModifier.PROTECTED;
import static com.tngtech.archunit.core.JavaModifier.PUBLIC;
import static com.tngtech.archunit.core.JavaModifier.STATIC;
import static com.tngtech.archunit.core.JavaModifier.TRANSIENT;
import static com.tngtech.archunit.core.JavaModifier.VOLATILE;
import static com.tngtech.archunit.core.JavaStaticInitializer.STATIC_INITIALIZER_NAME;
import static com.tngtech.archunit.core.ReflectionUtilsTest.constructor;
import static com.tngtech.archunit.core.ReflectionUtilsTest.field;
import static com.tngtech.archunit.core.ReflectionUtilsTest.method;
import static com.tngtech.archunit.core.testexamples.SomeEnum.SOME_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

public class ClassFileImporterTest {

    @Test
    public void imports_simple_classes() throws Exception {
        Set<String> expectedClassNames = Sets.newHashSet(
                ClassToImportOne.class.getName(), ClassToImportTwo.class.getName(), Object.class.getName());

        Iterable<JavaClass> classes = classesIn("testexamples/simpleimport");

        assertThat(namesOf(classes)).isEqualTo(expectedClassNames);
    }

    @Test
    public void imports_nested_classes() throws Exception {
        Set<String> expectedClassNames = Sets.newHashSet(
                ClassWithNestedClass.class.getName(),
                ClassWithNestedClass.NestedClass.class.getName(),
                ClassWithNestedClass.StaticNestedClass.class.getName(),
                ClassWithNestedClass.class.getName() + "$PrivateNestedClass",
                Object.class.getName());

        Iterable<JavaClass> classes = classesIn("testexamples/nestedimport");

        assertThat(namesOf(classes)).isEqualTo(expectedClassNames);
    }

    @Test
    public void imports_fields() throws Exception {
        Set<JavaField> fields = fieldsOf(classesIn("testexamples/fieldimport"));

        assertThat(namesOf(fields)).containsOnly("stringField", "serializableField", "objectField");
        assertThat(getByName(fields, "stringField").getType()).isEqualTo(String.class);
        assertThat(getByName(fields, "serializableField").getType()).isEqualTo(Serializable.class);
        assertThat(getByName(fields, "objectField").getType()).isEqualTo(Object.class);
    }

    @Test
    public void imports_primitive_fields() throws Exception {
        Set<JavaField> fields = fieldsOf(classesIn("testexamples/primitivefieldimport"));

        assertThat(getByName(fields, "aBoolean").getType()).isEqualTo(boolean.class);
        assertThat(getByName(fields, "anInt").getType()).isEqualTo(int.class);
        assertThat(getByName(fields, "aByte").getType()).isEqualTo(byte.class);
        assertThat(getByName(fields, "aChar").getType()).isEqualTo(char.class);
        assertThat(getByName(fields, "aShort").getType()).isEqualTo(short.class);
        assertThat(getByName(fields, "aLong").getType()).isEqualTo(long.class);
        assertThat(getByName(fields, "aFloat").getType()).isEqualTo(float.class);
        assertThat(getByName(fields, "aDouble").getType()).isEqualTo(double.class);
    }

    @Test
    public void attaches_correct_owner_to_fields() throws Exception {
        Iterable<JavaClass> classes = classesIn("testexamples/fieldimport");

        for (JavaClass clazz : classes) {
            for (JavaField field : clazz.getFields()) {
                assertThat(field.getOwner()).isSameAs(clazz);
            }
        }
    }

    @Test
    public void imports_fields_with_correct_modifiers() throws Exception {
        Set<JavaField> fields = fieldsOf(classesIn("testexamples/modifierfieldimport"));

        assertThat(getByName(fields, "privateField").getModifiers()).containsOnly(PRIVATE);
        assertThat(getByName(fields, "defaultField").getModifiers()).isEmpty();
        assertThat(getByName(fields, "privateFinalField").getModifiers()).containsOnly(PRIVATE, FINAL);
        assertThat(getByName(fields, "privateStaticField").getModifiers()).containsOnly(PRIVATE, STATIC);
        assertThat(getByName(fields, "privateStaticFinalField").getModifiers()).containsOnly(PRIVATE, STATIC, FINAL);
        assertThat(getByName(fields, "staticDefaultField").getModifiers()).containsOnly(STATIC);
        assertThat(getByName(fields, "protectedField").getModifiers()).containsOnly(PROTECTED);
        assertThat(getByName(fields, "protectedFinalField").getModifiers()).containsOnly(PROTECTED, FINAL);
        assertThat(getByName(fields, "publicField").getModifiers()).containsOnly(PUBLIC);
        assertThat(getByName(fields, "publicStaticFinalField").getModifiers()).containsOnly(PUBLIC, STATIC, FINAL);
        assertThat(getByName(fields, "volatileField").getModifiers()).containsOnly(VOLATILE);
        assertThat(getByName(fields, "synchronizedField").getModifiers()).containsOnly(TRANSIENT);
    }

    @Test
    public void imports_fields_with_one_annotation_correctly() throws Exception {
        Set<JavaField> fields = fieldsOf(classesIn("testexamples/annotationfieldimport"));

        JavaField field = getByName(fields, "stringAnnotatedField");
        JavaAnnotation<?> annotation = field.getAnnotationOfType(FieldAnnotationWithStringValue.class);
        assertThat(annotation.getType()).isEqualTo(FieldAnnotationWithStringValue.class);

        FieldAnnotationWithStringValue rawAnnotation = field.getReflectionAnnotationOfType(FieldAnnotationWithStringValue.class);
        assertThat(rawAnnotation.value()).isEqualTo("something");
    }

    @Test
    public void fields_handle_optional_annotation_correctly() throws Exception {
        Set<JavaField> fields = fieldsOf(classesIn("testexamples/annotationfieldimport"));

        JavaField field = getByName(fields, "stringAnnotatedField");
        assertThat(field.tryGetAnnotationOfType(FieldAnnotationWithStringValue.class)).isPresent();
        assertThat(field.tryGetReflectionAnnotationOfType(FieldAnnotationWithStringValue.class)).isPresent();
        assertThat(field.tryGetAnnotationOfType(FieldAnnotationWithEnumAndArrayValue.class)).isAbsent();
        assertThat(field.tryGetReflectionAnnotationOfType(FieldAnnotationWithEnumAndArrayValue.class)).isAbsent();
    }

    @Test
    public void imports_fields_with_two_annotations_correctly() throws Exception {
        Set<JavaField> fields = fieldsOf(classesIn("testexamples/annotationfieldimport"));

        JavaField field = getByName(fields, "stringAndIntAnnotatedField");
        assertThat(field.getAnnotations()).hasSize(2);

        FieldAnnotationWithStringValue annotationWithString = field.getReflectionAnnotationOfType(FieldAnnotationWithStringValue.class);
        assertThat(annotationWithString.value()).isEqualTo("otherThing");

        FieldAnnotationWithIntValue annotationWithInt = field.getReflectionAnnotationOfType(FieldAnnotationWithIntValue.class);
        assertThat(annotationWithInt.intValue()).isEqualTo(0);
        assertThat(annotationWithInt.otherValue()).isEqualTo("overridden");
    }

    @Test
    public void sets_owner_of_field_annotations_correctly() throws Exception {
        Iterable<JavaClass> classes = classesIn("testexamples/annotationfieldimport");

        for (JavaClass clazz : classes) {
            for (JavaField field : clazz.getFields()) {
                for (JavaAnnotation<?> annotation : field.getAnnotations()) {
                    assertThat(annotation.getOwner()).isSameAs(field);
                }
            }
        }
    }

    @Test
    public void imports_fields_with_complex_annotations_correctly() throws Exception {
        Set<JavaField> fields = fieldsOf(classesIn("testexamples/annotationfieldimport"));

        JavaField field = getByName(fields, "enumAndArrayAnnotatedField");

        FieldAnnotationWithEnumAndArrayValue annotation = field.getReflectionAnnotationOfType(FieldAnnotationWithEnumAndArrayValue.class);
        assertThat(annotation.value()).isEqualTo(SOME_VALUE);
        assertThat(annotation.classes()).containsExactly(Object.class, Serializable.class);
    }

    @Test
    public void imports_methods_with_correct_parameters() throws Exception {
        Set<JavaMethodLike<?, ?>> methods = methodsOf(classesIn("testexamples/methodimport"));

        assertThat(getByName(methods, "createString").getParameters())
                .as("Parameters of method 'createString'")
                .containsExactly(String.class);
        assertThat(getByName(methods, "consume").getParameters())
                .as("Parameters of method 'consume'")
                .containsExactly(Object.class);
        assertThat(getByName(methods, "createSerializable").getParameters())
                .as("Parameters of method 'createSerializable'")
                .containsExactly(int.class, int.class);
    }

    @Test
    public void imports_complex_method_with_correct_parameters() throws Exception {
        Set<JavaMethodLike<?, ?>> methods = methodsOf(classesIn("testexamples/complexmethodimport"));

        assertThat(getByName(methods, "complex").getParameters())
                .as("Parameters of method 'complex'")
                .containsExactly(
                        String.class,
                        long.class,
                        long.class,
                        Serializable.class,
                        Serializable.class);
    }

    @Test
    public void imports_methods_with_correct_return_types() throws Exception {
        Set<JavaMethodLike<?, ?>> methods = methodsOf(classesIn("testexamples/methodimport"));

        assertThat(getByName(methods, "createString").getReturnType())
                .as("Parameters of method 'createString'")
                .isEqualTo(String.class);
        assertThat(getByName(methods, "consume").getReturnType())
                .as("Parameters of method 'consume'")
                .isEqualTo(void.class);
        assertThat(getByName(methods, "createSerializable").getReturnType())
                .as("Parameters of method 'createSerializable'")
                .isEqualTo(Serializable.class);
    }

    @Test
    public void imports_methods_with_one_annotation_correctly() throws Exception {
        Set<JavaMethodLike<?, ?>> methods = methodsOf(classesIn("testexamples/annotationmethodimport"));

        JavaMethodLike<?, ?> method = getByName(methods, "stringAnnotatedMethod");
        JavaAnnotation<?> annotation = method.getAnnotationOfType(MethodAnnotationWithStringValue.class);
        assertThat(annotation.getType()).isEqualTo(MethodAnnotationWithStringValue.class);

        MethodAnnotationWithStringValue rawAnnotation = method.getReflectionAnnotationOfType(MethodAnnotationWithStringValue.class);
        assertThat(rawAnnotation.value()).isEqualTo("something");
    }

    @Test
    public void methods_handle_optional_annotation_correctly() throws Exception {
        Set<JavaMethodLike<?, ?>> methods = methodsOf(classesIn("testexamples/annotationmethodimport"));

        JavaMethodLike<?, ?> method = getByName(methods, "stringAnnotatedMethod");
        assertThat(method.tryGetAnnotationOfType(MethodAnnotationWithStringValue.class)).isPresent();
        assertThat(method.tryGetReflectionAnnotationOfType(MethodAnnotationWithStringValue.class)).isPresent();
        assertThat(method.tryGetAnnotationOfType(MethodAnnotationWithEnumAndArrayValue.class)).isAbsent();
        assertThat(method.tryGetReflectionAnnotationOfType(MethodAnnotationWithEnumAndArrayValue.class)).isAbsent();
    }

    @Test
    public void imports_methods_with_two_annotations_correctly() throws Exception {
        Iterable<JavaClass> classes = classesIn("testexamples/annotationmethodimport");

        Set<JavaMethodLike<?, ?>> methods = methodsOf(classes);

        JavaMethodLike<?, ?> method = getByName(methods, "stringAndIntAnnotatedMethod");
        assertThat(method.getAnnotations()).hasSize(2);

        MethodAnnotationWithStringValue annotationWithString = method.getReflectionAnnotationOfType(MethodAnnotationWithStringValue.class);
        assertThat(annotationWithString.value()).isEqualTo("otherThing");

        MethodAnnotationWithIntValue annotationWithInt = method.getReflectionAnnotationOfType(MethodAnnotationWithIntValue.class);
        assertThat(annotationWithInt.intValue()).isEqualTo(0);
        assertThat(annotationWithInt.otherValue()).isEqualTo("overridden");
    }

    @Test
    public void sets_owner_of_annotations_correctly() throws Exception {
        Iterable<JavaClass> classes = classesIn("testexamples/annotationmethodimport");

        for (JavaClass clazz : classes) {
            for (JavaMethodLike<?, ?> method : clazz.getMethods()) {
                for (JavaAnnotation<?> annotation : method.getAnnotations()) {
                    assertThat(annotation.getOwner()).isSameAs(method);
                }
            }
        }
    }

    @Test
    public void imports_methods_with_complex_annotations_correctly() throws Exception {
        Iterable<JavaClass> classes = classesIn("testexamples/annotationmethodimport");

        Set<JavaMethodLike<?, ?>> methods = methodsOf(classes);

        JavaMethodLike<?, ?> method = getByName(methods, "enumAndArrayAnnotatedMethod");

        MethodAnnotationWithEnumAndArrayValue annotation = method.getReflectionAnnotationOfType(MethodAnnotationWithEnumAndArrayValue.class);
        assertThat(annotation.value()).isEqualTo(SOME_VALUE);
        assertThat(annotation.classes()).containsExactly(Object.class, Serializable.class);
    }

    @Test
    public void imports_constructors_with_complex_annotations_correctly() throws Exception {
        JavaConstructor constructor = classesIn("testexamples/annotationmethodimport").get(ClassWithAnnotatedMethods.class)
                .getConstructor();

        MethodAnnotationWithEnumAndArrayValue annotation = constructor.getReflectionAnnotationOfType(MethodAnnotationWithEnumAndArrayValue.class);
        assertThat(annotation.value()).isEqualTo(SOME_VALUE);
        assertThat(annotation.classes()).containsExactly(Object.class, Serializable.class);
    }

    @Test
    public void imports_base_class_in_class_hierarchy_correctly() throws Exception {
        JavaClass baseClass = classesIn("testexamples/classhierarchyimport").get(BaseClass.class);

        assertThat(baseClass.getConstructors()).hasSize(2);
        assertThat(baseClass.getFields()).hasSize(1);
        assertThat(baseClass.getProperMethods()).hasSize(2);
        assertThat(baseClass.getStaticInitializer().getProperMethodCalls().size()).isGreaterThan(0);
    }

    @Test
    public void imports_sub_class_in_class_hierarchy_correctly() throws Exception {
        JavaClass subClass = classesIn("testexamples/classhierarchyimport").get(SubClass.class);

        assertThat(subClass.getConstructors()).hasSize(3);
        assertThat(subClass.getFields()).hasSize(1);
        assertThat(subClass.getProperMethods()).hasSize(3);
        assertThat(subClass.getStaticInitializer().getProperMethodCalls().size()).isGreaterThan(0);
    }

    @Test
    public void creates_relations_between_super_and_sub_classes() throws Exception {
        ImportedClasses classes = classesIn("testexamples/classhierarchyimport");
        JavaClass baseClass = classes.get(BaseClass.class);
        JavaClass subClass = classes.get(SubClass.class);
        JavaClass otherSubClass = classes.get(OtherSubClass.class);
        JavaClass subSubClass = classes.get(SubSubClass.class);

        assertThat(baseClass.getSubClasses()).containsOnly(subClass, otherSubClass);
        assertThat(baseClass.getAllSubClasses()).containsOnly(subClass, otherSubClass, subSubClass);
        assertThat(subClass.getSuperClass()).contains(baseClass);
        assertThat(subClass.getAllSubClasses()).containsOnly(subSubClass);
        assertThat(subSubClass.getSuperClass()).contains(subClass);
    }

    @Test
    public void imports_enclosing_classes() throws Exception {
        ImportedClasses classes = classesIn("testexamples/innerclassimport");
        JavaClass classWithInnerClass = classes.get(ClassWithInnerClass.class);
        JavaClass innerClass = classes.get(ClassWithInnerClass.Inner.class);
        JavaClass anonymousClass = classes.get(ClassWithInnerClass.class.getName() + "$1");
        JavaMethod calledTarget = getOnlyElement(classes.get(CalledClass.class).getProperMethods());

        assertThat(innerClass.getEnclosingClass()).contains(classWithInnerClass);
        assertThat(anonymousClass.getEnclosingClass()).contains(classWithInnerClass);

        JavaMethodCall call = getOnlyElement(innerClass.getMethod("call").getProperMethodCalls());

        assertThatCall(call).isFromMethod("call").isTo(calledTarget).inLineNumber(20);
        call = getOnlyElement(anonymousClass.getMethod("call").getProperMethodCalls());

        assertThatCall(call).isFromMethod("call").isTo(calledTarget).inLineNumber(10);
    }

    @Test
    public void imports_overridden_methods_correctly() throws Exception {
        ImportedClasses classes = classesIn("testexamples/classhierarchyimport");
        JavaClass baseClass = classes.get(BaseClass.class);
        JavaClass subClass = classes.get(SubClass.class);

        assertThat(baseClass.getMethod("getSomeField").getModifiers()).containsOnly(PROTECTED);
        assertThat(subClass.getMethod("getSomeField").getModifiers()).containsOnly(PUBLIC);
    }

    @Test
    public void imports_own_get_field_access() throws Exception {
        JavaClass classWithOwnFieldAccess = classesIn("testexamples/fieldaccessimport").get(OwnFieldAccess.class);

        JavaMethod getStringValue = classWithOwnFieldAccess.getProperMethod("getStringValue");

        JavaFieldAccess access = getOnlyElement(getStringValue.getFieldAccesses());
        assertThatAccess(access)
                .isOfType(GET)
                .isFromMethod(getStringValue)
                .isTo("stringValue")
                .inLineNumber(8);
    }

    @Test
    public void imports_own_set_field_access() throws Exception {
        JavaClass classWithOwnFieldAccess = classesIn("testexamples/fieldaccessimport").get(OwnFieldAccess.class);

        JavaMethod setStringValue = classWithOwnFieldAccess.getProperMethod("setStringValue", String.class);

        JavaFieldAccess access = getOnlyElement(setStringValue.getFieldAccesses());
        assertThatAccess(access)
                .isOfType(SET)
                .isFromMethod(setStringValue)
                .isTo(classWithOwnFieldAccess.getField("stringValue"))
                .inLineNumber(12);
    }

    @Test
    public void imports_multiple_own_accesses() throws Exception {
        JavaClass classWithOwnFieldAccess = classesIn("testexamples/fieldaccessimport").get(OwnFieldAccess.class);

        Set<JavaFieldAccess> fieldAccesses = classWithOwnFieldAccess.getFieldAccesses();

        assertThat(fieldAccesses).hasSize(4);
        assertThat(getOnly(fieldAccesses, "stringValue", GET).getLineNumber())
                .as("Line number of get stringValue").isEqualTo(8);
        assertThat(getOnly(fieldAccesses, "stringValue", SET).getLineNumber())
                .as("Line number of set stringValue").isEqualTo(12);
        assertThat(getOnly(fieldAccesses, "intValue", GET).getLineNumber())
                .as("Line number of get intValue").isEqualTo(16);
        assertThat(getOnly(fieldAccesses, "intValue", SET).getLineNumber())
                .as("Line number of set intValue").isEqualTo(20);
    }

    @Test
    public void imports_own_static_field_accesses() throws Exception {
        JavaClass classWithOwnFieldAccess = classesIn("testexamples/fieldaccessimport").get(OwnStaticFieldAccess.class);

        Set<JavaFieldAccess> accesses = classWithOwnFieldAccess.getFieldAccesses();

        assertThat(accesses).hasSize(2);

        JavaFieldAccess getAccess = getOnly(accesses, "staticStringValue", GET);

        assertThatAccess(getAccess)
                .isFromMethod("getStaticStringValue")
                .isTo("staticStringValue")
                .inLineNumber(7);

        JavaFieldAccess setAccess = getOnly(accesses, "staticStringValue", SET);

        assertThatAccess(setAccess)
                .isFromMethod("setStaticStringValue", String.class)
                .isTo("staticStringValue")
                .inLineNumber(11);
    }

    @Test
    public void imports_other_field_accesses() throws Exception {
        ImportedClasses classes = classesIn("testexamples/fieldaccessimport");
        JavaClass classWithOwnFieldAccess = classes.get(OwnFieldAccess.class);
        JavaClass classWithForeignFieldAccess = classes.get(ForeignFieldAccess.class);

        Set<JavaFieldAccess> accesses = classWithForeignFieldAccess.getFieldAccesses();

        assertThat(accesses).hasSize(4);

        assertThatAccess(getOnly(accesses, "stringValue", GET))
                .isFromMethod(classWithForeignFieldAccess.getMethod("getStringFromOther"))
                .isTo(classWithOwnFieldAccess.getField("stringValue"))
                .inLineNumber(5);

        assertThatAccess(getOnly(accesses, "stringValue", SET))
                .isFromMethod(classWithForeignFieldAccess.getMethod("setStringFromOther"))
                .isTo(classWithOwnFieldAccess.getField("stringValue"))
                .inLineNumber(9);

        assertThatAccess(getOnly(accesses, "intValue", GET))
                .isFromMethod(classWithForeignFieldAccess.getMethod("getIntFromOther"))
                .isTo(classWithOwnFieldAccess.getField("intValue"))
                .inLineNumber(13);

        assertThatAccess(getOnly(accesses, "intValue", SET))
                .isFromMethod(classWithForeignFieldAccess.getMethod("setIntFromOther"))
                .isTo(classWithOwnFieldAccess.getField("intValue"))
                .inLineNumber(17);
    }

    @Test
    public void imports_other_static_field_accesses() throws Exception {
        ImportedClasses classes = classesIn("testexamples/fieldaccessimport");
        JavaClass classWithOwnFieldAccess = classes.get(OwnStaticFieldAccess.class);
        JavaClass classWithForeignFieldAccess = classes.get(ForeignStaticFieldAccess.class);

        Set<JavaFieldAccess> accesses = classWithForeignFieldAccess.getFieldAccesses();

        assertThat(accesses).as("Number of field accesses from " + classWithForeignFieldAccess.getName()).hasSize(2);

        assertThatAccess(getOnly(accesses, "staticStringValue", GET))
                .isFromMethod(classWithForeignFieldAccess.getMethod("getStaticStringFromOther"))
                .isTo(classWithOwnFieldAccess.getField("staticStringValue"))
                .inLineNumber(5);

        assertThatAccess(getOnly(accesses, "staticStringValue", SET))
                .isFromMethod(classWithForeignFieldAccess.getMethod("setStaticStringFromOther"))
                .isTo(classWithOwnFieldAccess.getField("staticStringValue"))
                .inLineNumber(9);
    }

    @Test
    public void imports_multiple_accesses_from_same_method() throws Exception {
        ImportedClasses classes = classesIn("testexamples/fieldaccessimport");
        JavaClass classWithOwnFieldAccess = classes.get(OwnFieldAccess.class);
        JavaClass multipleFieldAccesses = classes.get(MultipleFieldAccessInSameMethod.class);

        Set<JavaFieldAccess> accesses = multipleFieldAccesses.getFieldAccesses();

        assertThat(accesses).as("Number of field accesses from " + multipleFieldAccesses.getName()).hasSize(5);

        Set<JavaFieldAccess> setStringValues = getByNameAndAccessType(accesses, "stringValue", SET);
        assertThat(setStringValues).hasSize(2);
        assertThat(targetsOf(setStringValues)).containsOnly(classWithOwnFieldAccess.getField("stringValue"));
        assertThat(lineNumbersOf(setStringValues)).containsOnly(6, 8);

        assertThatAccess(getOnly(accesses, "stringValue", GET))
                .isTo(classWithOwnFieldAccess.getField("stringValue"))
                .inLineNumber(7);

        assertThatAccess(getOnly(accesses, "intValue", GET))
                .isTo(classWithOwnFieldAccess.getField("intValue"))
                .inLineNumber(10);

        assertThatAccess(getOnly(accesses, "intValue", SET))
                .isTo(classWithOwnFieldAccess.getField("intValue"))
                .inLineNumber(11);
    }

    @Test
    public void imports_other_field_accesses_from_constructor() throws Exception {
        ImportedClasses classes = classesIn("testexamples/fieldaccessimport");
        JavaClass classWithOwnFieldAccess = classes.get(OwnFieldAccess.class);
        JavaClass fieldAccessFromConstructor = classes.get(ForeignFieldAccessFromConstructor.class);

        Set<JavaFieldAccess> accesses = fieldAccessFromConstructor.getFieldAccesses();

        assertThat(accesses).as("Number of field accesses from " + fieldAccessFromConstructor.getName()).hasSize(2);

        assertThatAccess(getOnly(accesses, "stringValue", GET))
                .isFromMethod(fieldAccessFromConstructor.getMethod(CONSTRUCTOR_NAME))
                .isTo(classWithOwnFieldAccess.getField("stringValue"))
                .inLineNumber(5);

        assertThatAccess(getOnly(accesses, "intValue", SET))
                .isFromMethod(fieldAccessFromConstructor.getMethod(CONSTRUCTOR_NAME))
                .isTo(classWithOwnFieldAccess.getField("intValue"))
                .inLineNumber(6);
    }

    @Test
    public void imports_other_field_accesses_from_static_initializer() throws Exception {
        ImportedClasses classes = classesIn("testexamples/fieldaccessimport");
        JavaClass classWithOwnFieldAccess = classes.get(OwnFieldAccess.class);
        JavaClass fieldAccessFromInitializer = classes.get(ForeignFieldAccessFromStaticInitializer.class);

        Set<JavaFieldAccess> accesses = fieldAccessFromInitializer.getFieldAccesses();

        assertThat(accesses).as("Number of field accesses from " + fieldAccessFromInitializer.getName()).hasSize(2);

        assertThatAccess(getOnly(accesses, "stringValue", GET))
                .isFromMethod(fieldAccessFromInitializer.getMethod(STATIC_INITIALIZER_NAME))
                .isTo(classWithOwnFieldAccess.getField("stringValue"))
                .inLineNumber(5);

        assertThatAccess(getOnly(accesses, "intValue", SET))
                .isFromMethod(fieldAccessFromInitializer.getMethod(STATIC_INITIALIZER_NAME))
                .isTo(classWithOwnFieldAccess.getField("intValue"))
                .inLineNumber(6);
    }

    @Test
    public void imports_external_field_access() throws Exception {
        JavaClass classWithExternalFieldAccess = classesIn("testexamples/fieldaccessimport").get(ExternalFieldAccess.class);

        JavaFieldAccess access = getOnlyElement(classWithExternalFieldAccess.getFieldAccesses());

        assertThatAccess(access)
                .isFromMethod(classWithExternalFieldAccess.getMethod("access"))
                .inLineNumber(7);

        JavaField target = access.getTarget();
        assertThat(target.getOwner().reflect()).isEqualTo(ClassWithIntAndObjectFields.class);
        assertThat(target.getFullName()).isEqualTo(ClassWithIntAndObjectFields.class.getName() + ".objectField");
    }

    @Test
    public void imports_external_field_access_with_shadowed_field() throws Exception {
        JavaClass classWithExternalFieldAccess = classesIn("testexamples/fieldaccessimport").get(ExternalShadowedFieldAccess.class);

        JavaFieldAccess access = getOnlyElement(classWithExternalFieldAccess.getFieldAccesses());

        assertThatAccess(access)
                .isFromMethod(classWithExternalFieldAccess.getMethod("accessField"))
                .inLineNumber(7);

        JavaField target = access.getTarget();
        assertThat(target.getOwner().reflect()).isEqualTo(ChildClass.class);
        assertThat(target.reflect()).isEqualTo(field(ChildClass.class, "someField"));
        assertThat(target.getFullName()).isEqualTo(ChildClass.class.getName() + ".someField");
    }

    @Test
    public void imports_shadowed_and_superclass_field_access() throws Exception {
        ImportedClasses classes = classesIn("testexamples/hierarchicalfieldaccess");
        JavaClass classThatAccessesFieldOfSuperClass = classes.get(AccessToSuperAndSubClassField.class);
        JavaClass superClassWithAccessedField = classes.get(SuperClassWithAccessedField.class);
        JavaClass subClassWithAccessedField = classes.get(SubClassWithAccessedField.class);

        Set<JavaFieldAccess> accesses = classThatAccessesFieldOfSuperClass.getFieldAccesses();

        assertThat(accesses).hasSize(2);
        assertThatAccess(getOnly(accesses, "field", GET))
                .isFromMethod("accessSuperClassField")
                .isTo(superClassWithAccessedField.getField("field"))
                .inLineNumber(5);
        assertThatAccess(getOnly(accesses, "maskedField", GET))
                .isFromMethod("accessSubClassField")
                .isTo(subClassWithAccessedField.getField("maskedField"))
                .inLineNumber(9);
    }

    @Test
    public void imports_shadowed_and_superclass_method_calls() throws Exception {
        ImportedClasses classes = classesIn("testexamples/hierarchicalmethodcall");
        JavaClass classThatCallsMethodOfSuperClass = classes.get(CallOfSuperAndSubClassMethod.class);
        JavaClass superClassWithCalledMethod = classes.get(SuperClassWithCalledMethod.class);
        JavaClass subClassWithCalledMethod = classes.get(SubClassWithCalledMethod.class);

        Set<JavaMethodCall> calls = classThatCallsMethodOfSuperClass.getProperMethodCalls();

        assertThat(calls).hasSize(2);

        JavaMethodLike<?, ?> callSuperClassMethod = classThatCallsMethodOfSuperClass
                .getMethod(CallOfSuperAndSubClassMethod.callSuperClassMethod);
        assertThatCall(getOnlyByCaller(calls, callSuperClassMethod))
                .isFromMethod(callSuperClassMethod)
                .isTo(superClassWithCalledMethod.getProperMethod(SuperClassWithCalledMethod.method))
                .inLineNumber(CallOfSuperAndSubClassMethod.callSuperClassLineNumber);

        JavaMethodLike<?, ?> callSubClassMethod = classThatCallsMethodOfSuperClass
                .getMethod(CallOfSuperAndSubClassMethod.callSubClassMethod);
        assertThatCall(getOnlyByCaller(calls, callSubClassMethod))
                .isFromMethod(callSubClassMethod)
                .isTo(subClassWithCalledMethod.getProperMethod(SubClassWithCalledMethod.maskedMethod))
                .inLineNumber(CallOfSuperAndSubClassMethod.callSubClassLineNumber);
    }

    @Test
    public void imports_constructor_calls_on_self() throws Exception {
        JavaClass classThatCallsOwnConstructor = classesIn("testexamples/callimport").get(CallsOwnConstructor.class);
        JavaMethodLike<?, ?> caller = classThatCallsOwnConstructor.getMethod("copy");

        Set<JavaConstructorCall> calls = classThatCallsOwnConstructor.getConstructorCalls();

        assertThatCall(getOnlyByCaller(calls, caller))
                .isFromMethod(caller)
                .isTo(classThatCallsOwnConstructor.getConstructor(String.class))
                .inLineNumber(8);
    }

    @Test
    public void imports_method_calls_on_self() throws Exception {
        JavaClass classThatCallsOwnMethod = classesIn("testexamples/callimport").get(CallsOwnMethod.class);

        JavaMethodCall call = getOnlyElement(classThatCallsOwnMethod.getProperMethodCalls());

        assertThatCall(call)
                .isFromMethod(classThatCallsOwnMethod.getMethod("getString"))
                .isTo(classThatCallsOwnMethod.getProperMethod("string"))
                .inLineNumber(6);
    }

    @Test
    public void imports_constructor_calls_on_other() throws Exception {
        ImportedClasses classes = classesIn("testexamples/callimport");
        JavaClass classThatCallsOtherConstructor = classes.get(CallsOtherConstructor.class);
        JavaClass otherClass = classes.get(CallsOwnConstructor.class);
        JavaMethodLike<?, ?> caller = classThatCallsOtherConstructor.getMethod("createOther");

        Set<JavaConstructorCall> calls = classThatCallsOtherConstructor.getConstructorCalls();

        assertThatCall(getOnlyByCaller(calls, caller))
                .isFromMethod(caller)
                .isTo(otherClass.getConstructor(String.class))
                .inLineNumber(5);
    }

    @Test
    public void imports_method_calls_on_other() throws Exception {
        ImportedClasses classes = classesIn("testexamples/callimport");
        JavaClass classThatCallsOtherMethod = classes.get(CallsOtherMethod.class);
        JavaClass other = classes.get(CallsOwnMethod.class);

        JavaMethodCall call = getOnlyElement(classThatCallsOtherMethod.getProperMethodCalls());

        assertThatCall(call)
                .isFromMethod(classThatCallsOtherMethod.getMethod("getFromOther"))
                .isTo(other.getProperMethod("getString"))
                .inLineNumber(7);
    }

    @Test
    public void imports_constructor_calls_on_external_class() throws Exception {
        JavaClass classThatCallsOwnConstructor = classesIn("testexamples/callimport").get(CallsOwnConstructor.class);
        JavaMethodLike<?, ?> constructorCallingObjectInit = classThatCallsOwnConstructor.getConstructor(String.class);

        JavaConstructorCall objectInitCall = getOnlyElement(constructorCallingObjectInit.getConstructorCalls());

        assertThatCall(objectInitCall)
                .isFromMethod(constructorCallingObjectInit)
                .inLineNumber(4);

        JavaConstructor target = objectInitCall.getTarget();
        assertThat(target.getFullName()).isEqualTo(Object.class.getName() + ".<init>()");
        assertThat(target.reflect()).isEqualTo(Object.class.getConstructor());
    }

    @Test
    public void imports_constructor_calls_to_sub_type_constructor_on_external_class() throws Exception {
        JavaClass classWithExternalConstructorCall = classesIn("testexamples/callimport").get(ExternalSubTypeConstructorCall.class);
        JavaMethodLike<?, ?> call = classWithExternalConstructorCall.getMethod("call");

        JavaConstructorCall callToExternalClass = getOnlyElement(call.getConstructorCalls());

        assertThatCall(callToExternalClass)
                .isFromMethod(call)
                .inLineNumber(7);

        JavaConstructor target = callToExternalClass.getTarget();
        assertThat(target.getFullName()).isEqualTo(ChildClass.class.getName() + ".<init>()");
        assertThat(target.reflect()).isEqualTo(constructor(ChildClass.class));
    }

    @Test
    public void imports_method_calls_on_external_class() throws Exception {
        JavaClass classThatCallsExternalMethod = classesIn("testexamples/callimport").get(CallsExternalMethod.class);

        JavaMethodCall call = getOnlyElement(classThatCallsExternalMethod.getProperMethodCalls());

        assertThatCall(call)
                .isFromMethod(classThatCallsExternalMethod.getMethod("getString"))
                .inLineNumber(7);

        JavaMethod target = call.getTarget();
        assertThat(target.getOwner().reflect()).isEqualTo(ArrayList.class);
        assertThat(target.getFullName()).isEqualTo(ArrayList.class.getName() + ".toString()");
    }

    @Test
    public void imports_method_calls_on_overridden_external_class() throws Exception {
        JavaClass classThatCallsExternalMethod = classesIn("testexamples/callimport").get(ExternalOverriddenMethodCall.class);

        JavaMethodCall call = getOnlyElement(classThatCallsExternalMethod.getProperMethodCalls());

        assertThatCall(call)
                .isFromMethod(classThatCallsExternalMethod.getMethod("call"))
                .inLineNumber(9);

        JavaMethod target = call.getTarget();
        assertThat(target.getFullName()).isEqualTo(ChildClass.class.getName() + ".overrideMe()");
        assertThat(target.reflect()).isEqualTo(method(ChildClass.class, "overrideMe"));
    }

    @Test
    public void imports_method_calls_on_external_interface_hierarchies() throws Exception {
        JavaClass classThatCallsExternalMethod = classesIn("testexamples/callimport").get(ExternalInterfaceMethodCall.class);

        JavaMethodCall call = getOnlyElement(classThatCallsExternalMethod.getProperMethodCalls());

        assertThatCall(call)
                .isFromMethod(classThatCallsExternalMethod.getMethod("call"))
                .inLineNumber(9);

        JavaMethod target = call.getTarget();
        assertThat(target.reflect()).isEqualTo(method(Map.class, "put", Object.class, Object.class));
    }

    @Test
    public void imports_non_unique_targets_for_diamond_scenarios() throws Exception {
        JavaClass classCallingDiamond = classesIn("testexamples/diamond").get(ClassCallingDiamond.class);
        JavaClass diamondPeakInterface = classesIn("testexamples/diamond").get(InterfaceD.class);
        JavaClass diamondPeakClass = classesIn("testexamples/diamond").get(ClassImplementingD.class);

        JavaMethodCalls calls = classCallingDiamond.getProperMethodCalls();

        assertThat(calls).hasSize(2);

        JavaMethodLike<?, ?> callInterface = classCallingDiamond
                .getMethod(ClassCallingDiamond.callInterface);
        JavaMethodCall callToInterface = getOnlyByCaller(calls, callInterface);
        assertThatCall(callToInterface)
                .isFromMethod(callInterface)
                .inLineNumber(ClassCallingDiamond.callInterfaceLineNumber);
        // NOTE: There is no 'proper' method InterfaceD.implementMe(), because the method is inherited
        assertThat(callToInterface.getTarget().getName()).isEqualTo(InterfaceD.implementMe);
        assertThat(callToInterface.getTarget().getOwner()).isEqualTo(diamondPeakInterface);
        assertThat(callToInterface.getTarget().getParameters()).isEmpty();

        JavaMethodLike<?, ?> callImplementation = classCallingDiamond
                .getMethod(ClassCallingDiamond.callImplementation);
        assertThatCall(getOnlyByCaller(calls, callImplementation))
                .isFromMethod(callImplementation)
                .isTo(diamondPeakClass.getProperMethod(InterfaceD.implementMe))
                .inLineNumber(ClassCallingDiamond.callImplementationLineNumber);
    }

    @Test
    public void imports_method_calls_that_return_Arrays() throws Exception {
        JavaClass classThatCallsMethodReturningArray = classesIn("testexamples/callimport").get(CallsMethodReturningArray.class);

        assertThat(classThatCallsMethodReturningArray.getProperMethodCalls())
                .extracting("target").extracting("owner").extracting("name")
                .containsOnly(SomeEnum.class.getName());
        assertThat(classThatCallsMethodReturningArray.getProperMethodCalls())
                .extracting("target").extracting("returnType")
                .containsOnly(SomeEnum[].class);
    }

    @Test
    public void dependency_target_classes_are_derived_correctly() throws Exception {
        ImportedClasses classes = classesIn("testexamples/integration");
        JavaClass javaClass = classes.get(ClassXDependingOnClassesABCD.class);
        Set<JavaClass> expectedTargetClasses = ImmutableSet.of(
                classes.get(ClassA.class),
                classes.get(ClassBDependingOnClassA.class),
                classes.get(ClassCDependingOnClassB.class),
                classes.get(ClassD.class)
        );

        Set<JavaClass> targetClasses = new HashSet<>();
        for (Dependency dependency : javaClass.getDirectDependencies()) {
            targetClasses.add(dependency.getTargetClass());
        }

        assertThat(targetClasses).isEqualTo(expectedTargetClasses);
    }

    @Test
    public void getDirectDependencies_does_not_return_transitive_dependencies() throws Exception {
        ImportedClasses classes = classesIn("testexamples/integration");
        JavaClass javaClass = classes.get(ClassCDependingOnClassB.class);
        JavaClass expectedTargetClass = classes.get(ClassBDependingOnClassA.class);

        Set<JavaClass> targetClasses = new HashSet<>();
        for (Dependency dependency : javaClass.getDirectDependencies()) {
            if (dependency.getTargetClass().getPackage().contains("testexamples")) {
                targetClasses.add(dependency.getTargetClass());
            }
        }

        assertThat(targetClasses).containsOnly(expectedTargetClass);
    }

    @Test
    public void imports_urls_of_files() {
        Set<URL> urls = newHashSet(urlOf(ClassToImportOne.class), urlOf(ClassWithNestedClass.class));

        JavaClasses classes = new ClassFileImporter().importUrls(urls);
        FluentIterable<JavaClass> classesFoundAtUrls = FluentIterable.from(classes)
                .filter(not(equalTo(new JavaClass.Builder().withType(Object.class).build())));

        assertThat(classesFoundAtUrls).as("Number of classes at the given URLs").hasSize(2);
    }

    @Test
    public void imports_urls_of_jars() throws IOException {
        Set<URL> urls = newHashSet(urlOf(Test.class), urlOf(RunWith.class));
        assumeTrue("We can't completely ensure, that this will always be taken from a JAR file, though it's very likely",
                JAR_PROTOCOL.equals(urls.iterator().next().getProtocol()));

        JavaClasses classes = new ClassFileImporter().importUrls(urls);

        assertThat(classes).as("Number of classes at the given URLs").hasSize(2);
    }

    private URL urlOf(Class<?> clazz) {
        return getClass().getResource("/" + clazz.getName().replace('.', '/') + ".class");
    }

    private JavaFieldAccess getOnly(Set<JavaFieldAccess> fieldAccesses, String name, AccessType accessType) {
        return getOnlyElement(getByNameAndAccessType(fieldAccesses, name, accessType));
    }

    private Set<JavaFieldAccess> getByNameAndAccessType(Set<JavaFieldAccess> fieldAccesses, String name, AccessType accessType) {
        Set<JavaFieldAccess> result = new HashSet<>();
        for (JavaFieldAccess access : fieldAccesses) {
            if (name.equals(access.getName()) && access.getAccessType() == accessType) {
                result.add(access);
            }
        }
        return result;
    }

    private <T extends IsOwnedByMethod> T getOnlyByCaller(Set<T> calls, JavaMethodLike<?, ?> caller) {
        return getOnlyElement(getByCaller(calls, caller));
    }

    private <T extends IsOwnedByMethod> Set<T> getByCaller(Set<T> calls, JavaMethodLike<?, ?> caller) {
        Set<T> result = new HashSet<>();
        for (T call : calls) {
            if (caller.equals(call.getOwner())) {
                result.add(call);
            }
        }
        return result;
    }

    private Set<JavaField> targetsOf(Set<JavaFieldAccess> fieldAccesses) {
        Set<JavaField> result = new HashSet<>();
        for (JavaFieldAccess access : fieldAccesses) {
            result.add(access.getTarget());
        }
        return result;
    }

    private Set<Integer> lineNumbersOf(Set<JavaFieldAccess> fieldAccesses) {
        Set<Integer> result = new HashSet<>();
        for (JavaFieldAccess access : fieldAccesses) {
            result.add(access.getLineNumber());
        }
        return result;
    }

    private Set<JavaField> fieldsOf(Iterable<JavaClass> classes) {
        Set<JavaField> fields = new HashSet<>();
        for (JavaClass clazz : classes) {
            fields.addAll(clazz.getFields());
        }
        return fields;
    }

    private Set<JavaMethodLike<?, ?>> methodsOf(Iterable<JavaClass> classes) {
        Set<JavaMethodLike<?, ?>> methods = new HashSet<>();
        for (JavaClass clazz : classes) {
            methods.addAll(clazz.getMethods());
        }
        return methods;
    }

    private Set<String> namesOf(Iterable<? extends HasName> thingsWithNames) {
        Set<String> result = new HashSet<>();
        for (HasName hasName : thingsWithNames) {
            result.add(hasName.getName());
        }
        return result;
    }

    private <T extends HasName> T getByName(Iterable<T> thingsWithName, String name) {
        for (T hasName : thingsWithName) {
            if (name.equals(hasName.getName())) {
                return hasName;
            }
        }
        throw new AssertionError("No object with name '" + name + "' is present in " + thingsWithName);
    }

    private ImportedClasses classesIn(String path) throws Exception {
        return new ImportedClasses(path);
    }

    private class ImportedClasses implements Iterable<JavaClass> {
        private final ClassFileImporter importer = new ClassFileImporter();
        private final Iterable<JavaClass> classes;

        private ImportedClasses(String path) throws Exception {
            classes = importer.importPath(Paths.get(ClassFileImporterTest.this.getClass().getResource(path).toURI()));
        }

        JavaClass get(Class<?> clazz) {
            return get(clazz.getName());
        }

        private JavaClass get(String className) {
            return getByName(classes, className);
        }

        @Override
        public Iterator<JavaClass> iterator() {
            return classes.iterator();
        }
    }

    private static AccessToFieldAssertion assertThatAccess(JavaFieldAccess access) {
        return new AccessToFieldAssertion(access);
    }

    private static MethodCallAssertion assertThatCall(JavaMethodCall call) {
        return new MethodCallAssertion(call);
    }

    private static ConstructorCallAssertion assertThatCall(JavaConstructorCall call) {
        return new ConstructorCallAssertion(call);
    }

    protected abstract static class BaseAccessAssertion<
            SELF extends BaseAccessAssertion<SELF, ACCESS, TARGET>,
            ACCESS extends JavaAccess<TARGET>,
            TARGET extends HasName.AndFullName & HasOwner<JavaClass>> {

        protected ACCESS access;

        protected BaseAccessAssertion(ACCESS access) {
            this.access = access;
        }

        protected SELF isFromMethod(String name, Class<?>... parameterTypes) {
            return isFromMethod(access.getOrigin().getOwner().getMethod(name, parameterTypes));
        }

        protected SELF isFromMethod(JavaMethodLike<?, ?> method) {
            assertThat(access.getOrigin()).as("Origin of field access").isEqualTo(method);
            return newAssertion(access);
        }

        protected SELF isTo(TARGET target) {
            assertThat(access.getTarget()).as("Target of " + access.getName()).isEqualTo(target);
            return newAssertion(access);
        }

        protected void inLineNumber(int number) {
            assertThat(access.getLineNumber())
                    .as("Line number of access to " + access.getName())
                    .isEqualTo(number);
        }

        protected abstract SELF newAssertion(ACCESS access);
    }

    private static class AccessToFieldAssertion extends BaseAccessAssertion<AccessToFieldAssertion, JavaFieldAccess, JavaField> {
        private AccessToFieldAssertion(JavaFieldAccess access) {
            super(access);
        }

        @Override
        protected AccessToFieldAssertion newAssertion(JavaFieldAccess access) {
            return new AccessToFieldAssertion(access);
        }

        private AccessToFieldAssertion isTo(String name) {
            return isTo(access.getOrigin().getOwner().getField(name));
        }

        private AccessToFieldAssertion isOfType(AccessType type) {
            assertThat(access.getAccessType()).isEqualTo(type);
            return newAssertion(access);
        }
    }

    private static class MethodCallAssertion extends BaseAccessAssertion<MethodCallAssertion, JavaMethodCall, JavaMethod> {
        private MethodCallAssertion(JavaMethodCall call) {
            super(call);
        }

        @Override
        protected MethodCallAssertion newAssertion(JavaMethodCall call) {
            return new MethodCallAssertion(call);
        }
    }

    private static class ConstructorCallAssertion extends BaseAccessAssertion<ConstructorCallAssertion, JavaConstructorCall, JavaConstructor> {
        private ConstructorCallAssertion(JavaConstructorCall call) {
            super(call);
        }

        @Override
        protected ConstructorCallAssertion newAssertion(JavaConstructorCall call) {
            return new ConstructorCallAssertion(call);
        }
    }
}