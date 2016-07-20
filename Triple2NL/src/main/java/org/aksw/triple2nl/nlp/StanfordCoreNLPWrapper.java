package org.aksw.triple2nl.nlp;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.AnnotationPipeline;

/**
 * A wrapper which allows to use either an in-memory or a server-based pipeline.
 *
 * @author Lorenz Buehmann
 */
public class StanfordCoreNLPWrapper extends AnnotationPipeline {

	private final AnnotationPipeline delegate;

	public StanfordCoreNLPWrapper(AnnotationPipeline delegate) {
		this.delegate = delegate;
	}

	@Override
	public void annotate(Annotation annotation) {
		delegate.annotate(annotation);
	}
}
