/******************************************************************************* 
 * Copyright (c) 2011 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdi.seam.solder.core;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.cdi.core.CDIConstants;
import org.jboss.tools.cdi.core.IRootDefinitionContext;
import org.jboss.tools.cdi.core.extension.ICDIExtension;
import org.jboss.tools.cdi.core.extension.feature.IProcessAnnotatedTypeFeature;
import org.jboss.tools.cdi.internal.core.impl.definition.AbstractMemberDefinition;
import org.jboss.tools.cdi.internal.core.impl.definition.AbstractTypeDefinition;
import org.jboss.tools.cdi.internal.core.impl.definition.AnnotationDefinition;
import org.jboss.tools.cdi.internal.core.impl.definition.FieldDefinition;
import org.jboss.tools.cdi.internal.core.impl.definition.MethodDefinition;
import org.jboss.tools.cdi.internal.core.impl.definition.PackageDefinition;
import org.jboss.tools.cdi.internal.core.impl.definition.ParameterDefinition;
import org.jboss.tools.cdi.internal.core.impl.definition.TypeDefinition;
import org.jboss.tools.common.java.IAnnotated;
import org.jboss.tools.common.java.IAnnotationDeclaration;
import org.jboss.tools.common.java.ParametedType;
import org.jboss.tools.common.java.TypeDeclaration;
import org.jboss.tools.common.java.impl.AnnotationLiteral;
import org.jboss.tools.common.util.BeanUtil;
import org.jboss.tools.common.util.EclipseJavaUtil;

/**
 * Implements support for org.jboss.seam.solder.core.CoreExtension
 * 
 * For @Veto and @Requires marks bean definition as vetoed.
 * 
 * For @FullyQualified and @Named on packages, adds fake @Named to bean
 * 
 * For @Exact marks parameter or field type as overridden.
 * 
 * @author Viacheslav Kabanovich
 *
 */
public class CDISeamSolderCoreExtension implements ICDIExtension, IProcessAnnotatedTypeFeature {

	public CDISeamSolderCoreExtension() {
	}

	protected Version getVersion() {
		return Version.instance;
	}

	public void processAnnotatedType(TypeDefinition typeDefinition, IRootDefinitionContext context) {

		if(processVeto(typeDefinition, context)) {
			return;
		}

		if(processRequires(typeDefinition, context)) {
			return;
		}
	
		processNames(typeDefinition, context);

		processExact(typeDefinition, context);

	}

	// @Veto
	private boolean processVeto(TypeDefinition typeDefinition, IRootDefinitionContext context) {
		if (typeDefinition
				.isAnnotationPresent(getVersion().getVetoAnnotationTypeName())
				|| (typeDefinition.getPackageDefinition() != null && typeDefinition
						.getPackageDefinition()
						.isAnnotationPresent(
								getVersion().getVetoAnnotationTypeName()))) {
			typeDefinition.veto();
			return true;
		}
		return false;
	}

	// @Requires
	private boolean processRequires(TypeDefinition typeDefinition, IRootDefinitionContext context) {
		Set<String> requiredClasses = new HashSet<String>();
		requiredClasses.addAll(getRequiredClasses(typeDefinition));
		PackageDefinition pkg = typeDefinition.getPackageDefinition();
		if(pkg != null) {
			requiredClasses.addAll(getRequiredClasses(pkg));
		}
		if (!requiredClasses.isEmpty()) {
			for (String c : requiredClasses) {
				if (context.getProject().getType(c) == null) {
					typeDefinition.veto();
					return true;
				}
			}
		}
		return false;
	}

	// @FullyQualified @Named
	private void processNames(TypeDefinition typeDefinition, IRootDefinitionContext context) {
		PackageDefinition p = typeDefinition.getPackageDefinition();
		IAnnotationDeclaration namedOnPackage = null;
		IAnnotationDeclaration fullyQualifiedOnPackage = null;
		if(p != null) {
			namedOnPackage = p.getAnnotation(CDIConstants.NAMED_QUALIFIER_TYPE_NAME);
			fullyQualifiedOnPackage = p.getAnnotation(getVersion().getFullyQualifiedAnnotationTypeName());
		}

		processNames(typeDefinition, context, namedOnPackage, fullyQualifiedOnPackage, p);
	
		List<FieldDefinition> fs = typeDefinition.getFields();
		for (FieldDefinition f: fs) {
			if(f.isAnnotationPresent(CDIConstants.PRODUCES_ANNOTATION_TYPE_NAME)) {
				processNames(f, context, null, fullyQualifiedOnPackage, p);
			}
		}
		
		List<MethodDefinition> ms = typeDefinition.getMethods();
		for (MethodDefinition m: ms) {
			if(m.isAnnotationPresent(CDIConstants.PRODUCES_ANNOTATION_TYPE_NAME)) {
				processNames(m, context, null, fullyQualifiedOnPackage, p);
			}
		}
		
	}

	private void processNames(AbstractMemberDefinition d, IRootDefinitionContext context,
			IAnnotationDeclaration namedOnPackage, IAnnotationDeclaration fullyQualifiedOnPackage, PackageDefinition p) {
		IAnnotationDeclaration named = d.getAnnotation(CDIConstants.NAMED_QUALIFIER_TYPE_NAME);
		IAnnotationDeclaration fullyQualified = d.getAnnotation(getVersion().getFullyQualifiedAnnotationTypeName());

		String beanName = null;

		if((fullyQualified != null || fullyQualifiedOnPackage != null) && (named != null || namedOnPackage != null)) {
			//@FullyQualified
			if(named == null) named = namedOnPackage;
			String pkg = resolvePackageName(fullyQualified, fullyQualifiedOnPackage, d.getTypeDefinition(), p);
			String simpleName = getSimpleBeanName(d, named);
			beanName = (simpleName == null) ? null : pkg.length() > 0 ? pkg + "." + simpleName : simpleName;			
		} else if(named == null && namedOnPackage != null) {
			// @Named on package only
			beanName = getSimpleBeanName(d, namedOnPackage);
		}
		
		if(beanName != null) {
			AnnotationDefinition n = context.getAnnotation(CDIConstants.NAMED_QUALIFIER_TYPE_NAME);
			if(n != null) {
				AnnotationLiteral l = new AnnotationLiteral(d.getResource(), 0, 0, beanName, IMemberValuePair.K_STRING, n.getType());
				if(named != null) d.removeAnnotation(named);
				d.addAnnotation(l, context);
			}
		}

	}

	private List<String> getRequiredClasses(IAnnotated d) {
		IAnnotationDeclaration requires = d
				.getAnnotation(getVersion().getRequiresAnnotationTypeName());
		return requires != null ? getArrayValue(requires) : new ArrayList<String>();
	}

	private List<String> getArrayValue(IAnnotationDeclaration d) {
		Object value = d.getMemberValue(null);
		List<String> result = new ArrayList<String>();
		if (value instanceof Object[]) {
			Object[] array = (Object[]) value;
			for (int i = 0; i < array.length; i++) {
				if (array[i] != null)
					result.add(array[i].toString());
			}
		} else if (value instanceof String) {
			result.add(value.toString());
		}
		return result;
	}

	// @Exact
	private void processExact(TypeDefinition typeDefinition, IRootDefinitionContext context) {
		List<FieldDefinition> fs = typeDefinition.getFields();
		for (FieldDefinition f : fs) {
			TypeDeclaration exact = getExactType(f, typeDefinition, context);
			if (exact != null) {
				f.setOverridenType(exact);
			}
		}

		List<MethodDefinition> ms = typeDefinition.getMethods();
		for (MethodDefinition m : ms) {
			List<ParameterDefinition> ps = m.getParameters();
			for (ParameterDefinition p : ps) {
				TypeDeclaration exact = getExactType(p, typeDefinition, context);
				if (exact != null) {
					p.setOverridenType(exact);
				}
			}
		}
	}

	private TypeDeclaration getExactType(IAnnotated annotated, TypeDefinition declaringType, IRootDefinitionContext context) {
		IAnnotationDeclaration a = annotated.getAnnotation(getVersion().getExactAnnotationTypeName());
		if(a != null) {
			Object o = a.getMemberValue(null);
			if(o != null) {
				String s = o.toString();
				if(s.length() > 0) {
					try {
						ParametedType p = context.getProject().getTypeFactory().getParametedType(declaringType.getType(), "Q" + s + ";");
						int b = a.getStartPosition();
						int e = b + a.getLength();
						if(b >= 0 && e > b) {
							String content = declaringType.getContent().substring(b, e);
							int i = content.indexOf(s);
							if(i >= 0) {
								b = i;
								e = i + s.length();
							}
						}
						return new TypeDeclaration(p, a.getResource(), b, e - b);
					} catch (JavaModelException e) {
						CDISeamSolderCorePlugin.getDefault().logError(e);
					}
				}
			}
		}
		return null;
	}

	private String resolvePackageName(IAnnotationDeclaration fullyQualified, IAnnotationDeclaration fullyQualifiedOnPackage, AbstractTypeDefinition t, PackageDefinition p) {
		String contextClass = null;
		IAnnotationDeclaration a = fullyQualified != null ? fullyQualified : fullyQualifiedOnPackage;
		if(a != null) {
			contextClass = getStringValue(a);
		}
		if(contextClass == null) {
			contextClass = t == null ? "" : t.getQualifiedName();
		} else if(fullyQualified != null && t != null) {
			String resolved = EclipseJavaUtil.resolveType(t.getType(), contextClass);
			if(resolved != null) contextClass = resolved;				
		} else if(fullyQualifiedOnPackage != null) {
			contextClass = p.resolveType(contextClass);
		}
		if("java.lang.Class".equals(contextClass)) {
			contextClass = t.getType().getFullyQualifiedName();
		}
		int dot = contextClass.lastIndexOf('.');
		return dot < 0 ? "" : contextClass.substring(0, dot);
	}

	private String getSimpleBeanName(AbstractMemberDefinition d, IAnnotationDeclaration named) {
		String simpleName = null;
		if(named != null) {
			simpleName = getStringValue(named);
		}
		if(simpleName != null && simpleName.length() > 0) {
			//do nothing
		} else if(d instanceof TypeDefinition) {
			simpleName = Introspector.decapitalize(((TypeDefinition)d).getType().getElementName());
		} else if(d instanceof FieldDefinition) {
			simpleName = ((FieldDefinition)d).getField().getElementName();
		} else if(d instanceof MethodDefinition) {
			MethodDefinition m = (MethodDefinition)d;
			String mn = m.getMethod().getElementName();
			if(BeanUtil.isGetter(m.getMethod())) {
				simpleName = BeanUtil.getPropertyName(mn);
			} else {
				simpleName = mn;
			}
		}
		
		return simpleName;
	}

	private String getStringValue(IAnnotationDeclaration a) {
		Object o = a.getMemberValue(null);
		return o == null ? null : o.toString();
	}

}
