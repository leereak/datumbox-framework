/**
 * Copyright (C) 2013-2016 Vasilis Vryniotis <bbriniotis@datumbox.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datumbox.framework.core.machinelearning.clustering;

import com.datumbox.framework.common.Configuration;
import com.datumbox.framework.common.dataobjects.Dataframe;
import com.datumbox.framework.core.machinelearning.MLBuilder;
import com.datumbox.framework.core.machinelearning.datatransformation.DummyXYMinMaxNormalizer;
import com.datumbox.framework.core.machinelearning.modelselection.metrics.ClusteringMetrics;
import com.datumbox.framework.core.machinelearning.modelselection.Validator;
import com.datumbox.framework.core.machinelearning.modelselection.splitters.KFoldSplitter;
import com.datumbox.framework.tests.Constants;
import com.datumbox.framework.tests.Datasets;
import com.datumbox.framework.tests.abstracts.AbstractTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test cases for Kmeans.
 *
 * @author Vasilis Vryniotis <bbriniotis@datumbox.com>
 */
public class KmeansTest extends AbstractTest {

    /**
     * Test of predict method, of class Kmeans.
     */
    @Test
    public void testPredict() {
        logger.info("testPredict");

        Configuration configuration = Configuration.getConfiguration();
        
        
        Dataframe[] data = Datasets.heartDiseaseClusters(configuration);
        
        Dataframe trainingData = data[0];
        Dataframe validationData = data[1];
        
        
        String storageName = this.getClass().getSimpleName();
        DummyXYMinMaxNormalizer df = MLBuilder.create(new DummyXYMinMaxNormalizer.TrainingParameters(), configuration);
        df.fit_transform(trainingData);
        df.save(storageName);

        
        Kmeans.TrainingParameters param = new Kmeans.TrainingParameters();
        param.setK(2);
        param.setMaxIterations(200);
        param.setInitializationMethod(Kmeans.TrainingParameters.Initialization.FORGY);
        param.setDistanceMethod(Kmeans.TrainingParameters.Distance.EUCLIDIAN);
        param.setWeighted(false);
        param.setCategoricalGamaMultiplier(1.0);
        param.setSubsetFurthestFirstcValue(2.0);

        Kmeans instance = MLBuilder.create(param, configuration);
        instance.fit(trainingData);
        instance.save(storageName);

        df.denormalize(trainingData);
        trainingData.close();
        
        instance.close();
        df.close();
        //instance = null;
        //df = null;
        
        df = MLBuilder.load(DummyXYMinMaxNormalizer.class, storageName, configuration);
        instance = MLBuilder.load(Kmeans.class, storageName, configuration);


        df.transform(validationData);
        instance.predict(validationData);
        ClusteringMetrics vm = new ClusteringMetrics(validationData);

        df.denormalize(validationData);

        double expResult = 1.0;
        double result = vm.getPurity();
        assertEquals(expResult, result, Constants.DOUBLE_ACCURACY_HIGH);
        
        df.delete();
        instance.delete();

        validationData.close();
    }

    
    /**
     * Test of validate method, of class Kmeans.
     */
    @Test
    public void testKFoldCrossValidation() {
        logger.info("testKFoldCrossValidation");
        
        Configuration configuration = Configuration.getConfiguration();
        
        int k = 5;
        
        Dataframe[] data = Datasets.heartDiseaseClusters(configuration);
        Dataframe trainingData = data[0];
        data[1].close();
        

        DummyXYMinMaxNormalizer df = MLBuilder.create(new DummyXYMinMaxNormalizer.TrainingParameters(), configuration);
        df.fit_transform(trainingData);
        

        
        

        
        Kmeans.TrainingParameters param = new Kmeans.TrainingParameters();
        param.setK(2);
        param.setMaxIterations(200);
        param.setInitializationMethod(Kmeans.TrainingParameters.Initialization.FORGY);
        param.setDistanceMethod(Kmeans.TrainingParameters.Distance.EUCLIDIAN); 
        param.setWeighted(false);
        param.setCategoricalGamaMultiplier(1.0);
        param.setSubsetFurthestFirstcValue(2.0);

        ClusteringMetrics vm = new Validator<>(ClusteringMetrics.class, configuration)
                .validate(new KFoldSplitter(k).split(trainingData), param);

        df.denormalize(trainingData);

        
        double expResult = 0.7888888888888889;
        double result = vm.getPurity();
        assertEquals(expResult, result, Constants.DOUBLE_ACCURACY_HIGH);
        
        df.close();
        
        trainingData.close();
    }

    
}
