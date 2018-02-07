package org.checkerframework.checker.nondeterminism;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.checkerframework.checker.initialization.InitializationVisitor;
import org.checkerframework.checker.nondeterminism.qual.Det;
import org.checkerframework.checker.nondeterminism.qual.Ond2D;
import org.checkerframework.checker.nondeterminism.qual.OrderNonDet;
import org.checkerframework.checker.nondeterminism.qual.PolyDet1Ond2D;
import org.checkerframework.checker.nondeterminism.qual.ValueNonDet;
import org.checkerframework.checker.nullness.NullnessAnnotatedTypeFactory;
import org.checkerframework.checker.nullness.NullnessStore;
import org.checkerframework.checker.nullness.NullnessValue;
import org.checkerframework.common.basetype.*;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;


public class NonDeterminismVisitor extends BaseTypeVisitor<BaseAnnotatedTypeFactory>{
	public NonDeterminismVisitor(BaseTypeChecker checker) {
		super(checker);
	}
	
	@Override
	public boolean isValidUse(AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
		// TODO Auto-generated method stub
		DeclaredType javaType = useType.getUnderlyingType();
		
		if(useType.hasAnnotation(AnnotationBuilder.fromClass(elements, OrderNonDet.class))) {
			if(!(isCollection(javaType.asElement().asType()))) 
				checker.report(Result.failure("OrderNonDet - can only be used with collections"), tree);
		}
		
		//Sets and lists 
		if(isCollection(javaType.asElement().asType())
				&& javaType.getTypeArguments().size() == 1) {
			AnnotationMirror baseAnnotation = useType.getAnnotations().iterator().next();
			AnnotatedTypeMirror paramType = useType.getTypeArguments().iterator().next();
			Iterator<AnnotationMirror> paramAnnotationIt = paramType.getAnnotations().iterator();
			if(paramAnnotationIt.hasNext()) {
				AnnotationMirror paramAnnotation = paramAnnotationIt.next();
				if(isAnnoSubType(baseAnnotation, paramAnnotation))
					checker.report(Result.failure(" Cannot have a " + baseAnnotation 
						+ " collection of " + paramAnnotation), tree);
			}
		}
		
		return super.isValidUse(declarationType, useType, tree);
	}
	
	private boolean isAnnoSubType(AnnotationMirror baseAnno, AnnotationMirror paramAnno) {
		AnnotationMirror DetTypeMirror = AnnotationBuilder.fromClass(elements, Det.class);
		AnnotationMirror OrderNonDetTypeMirror = AnnotationBuilder.fromClass(elements, OrderNonDet.class);
		AnnotationMirror ValueNonDetTypeMirror = AnnotationBuilder.fromClass(elements, ValueNonDet.class);
		
		if(types.isSameType(baseAnno.getAnnotationType(), OrderNonDetTypeMirror.getAnnotationType()) 
				&& types.isSameType(paramAnno.getAnnotationType(), ValueNonDetTypeMirror.getAnnotationType())) {
			return true;
		}
		
		if(types.isSameType(baseAnno.getAnnotationType(), DetTypeMirror.getAnnotationType())) {
			if(types.isSameType(paramAnno.getAnnotationType(), OrderNonDetTypeMirror.getAnnotationType()) || 
					types.isSameType(paramAnno.getAnnotationType(), ValueNonDetTypeMirror.getAnnotationType()))
				return true;
		}	
		return false;
	}
	
	private boolean isCollection(TypeMirror tm) {
		ProcessingEnvironment processingEnvironment = checker.getProcessingEnvironment();
		javax.lang.model.util.Types types = processingEnvironment.getTypeUtils();
		
		TypeMirror ListTypeMirror =
                TypesUtils.typeFromClass(
                        List.class, types, processingEnvironment.getElementUtils());
		TypeMirror ArrayListTypeMirror =
                TypesUtils.typeFromClass(
                        ArrayList.class, types, processingEnvironment.getElementUtils());
		TypeMirror SetTypeMirror =
                TypesUtils.typeFromClass(
                        Set.class, types, processingEnvironment.getElementUtils());
		TypeMirror HashSetTypeMirror =
                TypesUtils.typeFromClass(
                        HashSet.class, types, processingEnvironment.getElementUtils());
		
		if(types.isSubtype(tm, ListTypeMirror)
			|| types.isSubtype(tm, SetTypeMirror)
			|| types.isSubtype(tm, ArrayListTypeMirror)
			|| types.isSubtype(tm, HashSetTypeMirror))
				return true;
		return false;
	}
	
	private boolean isList(TypeMirror tm) {
		ProcessingEnvironment processingEnvironment = checker.getProcessingEnvironment();
		javax.lang.model.util.Types types = processingEnvironment.getTypeUtils();
		
		TypeMirror ListTypeMirror =
                TypesUtils.typeFromClass(
                        List.class, types, processingEnvironment.getElementUtils());
		TypeMirror ArrayListTypeMirror =
                TypesUtils.typeFromClass(
                        ArrayList.class, types, processingEnvironment.getElementUtils());
		
		
		if(types.isSubtype(tm, ListTypeMirror)
			|| types.isSubtype(tm, ArrayListTypeMirror))
				return true;
		return false;
	}
	
	@Override
	public boolean isValidUse(AnnotatedPrimitiveType type, Tree tree) {
		// TODO Auto-generated method stub
		Set<AnnotationMirror> annos =  type.getAnnotations();
		if(annos.contains(AnnotationBuilder.fromClass(elements, OrderNonDet.class)))
			checker.report(Result.failure("OrderNonDet - can only be used with collections"), tree);
		return super.isValidUse(type, tree);
	}
	
	@Override
	public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
		// TODO Auto-generated method stub
		Tree receiverTree = TreeUtils.getReceiverTree(node);
		if(receiverTree != null) {
			if(atypeFactory.getAnnotatedType(node).getAnnotations().size() > 0) {
				AnnotationMirror returnAnno = atypeFactory.getAnnotatedType(node).getAnnotations().iterator().next();
				checker.report(Result.warning(">>>  " + returnAnno + " " + atypeFactory.getAnnotatedType(node).getAnnotations().size()), node);
				AnnotationMirror Ond2DTypeMirror = AnnotationBuilder.fromClass(elements, Ond2D.class);
				if(types.isSameType(returnAnno.getAnnotationType(), Ond2DTypeMirror.getAnnotationType())) {
					TypeMirror receiverType = atypeFactory.getNodeForTree(receiverTree).getType();
					AnnotationMirror receiverAnno = atypeFactory.getAnnotatedType(receiverTree).getAnnotations().iterator().next();
					AnnotationMirror OrderNonDetTypeMirror = AnnotationBuilder.fromClass(elements, OrderNonDet.class);
					if(types.isSameType(receiverAnno.getAnnotationType(), OrderNonDetTypeMirror.getAnnotationType())) {
						AnnotatedTypeMirror atm = atypeFactory.getAnnotatedType(node);
						atm.clearAnnotations();
						checker.report(Result.warning("old anno " + atm.getAnnotations().size()), node);
						atm.addAnnotation(AnnotationBuilder.fromClass(elements, Det.class));
						checker.report(Result.warning("new anno" + atm.getAnnotations().iterator().next()), node);
					}
				}
			}
		}
		return super.visitMethodInvocation(node, p);
	}
	
	/*@Override
	protected void commonAssignmentCheck(AnnotatedTypeMirror varType, AnnotatedTypeMirror valueType, Tree valueTree,
			String errorKey) {
		// TODO Auto-generated method stub
		if(valueTree.getKind() == Kind.METHOD_INVOCATION) {
			checker.report(Result.warning("Really? " + varType + " = " + valueType.getAnnotations()), valueTree);
			//if(valueTree != null) {
				if(valueType.getAnnotations().size() > 0) {
					AnnotationMirror returnAnno = valueType.getAnnotations().iterator().next();
					AnnotationMirror Ond2DTypeMirror = AnnotationBuilder.fromClass(elements, Ond2D.class);
					if(types.isSameType(returnAnno.getAnnotationType(), Ond2DTypeMirror.getAnnotationType())) {
						AnnotatedTypeMirror atm = valueType;
						atm.clearAnnotations();
						checker.report(Result.warning("old " + atm.getAnnotations().size()), valueTree);
						atm.addAnnotation(AnnotationBuilder.fromClass(elements, Det.class));
						checker.report(Result.warning("new " + atm.getAnnotations().iterator().next()), valueTree);
					}
				}
			//}
		}
		super.commonAssignmentCheck(varType, valueType, valueTree, errorKey);
	}*/
	
	@Override
	protected TypeValidator createTypeValidator() {
		// TODO Auto-generated method stub
		return new BaseTypeValidator(checker, this, atypeFactory) {
			@Override
			protected void reportInvalidAnnotationsOnUse(AnnotatedTypeMirror type, Tree p) {
				
				// TODO Auto-generated method stub
			}
		};
	}
}

