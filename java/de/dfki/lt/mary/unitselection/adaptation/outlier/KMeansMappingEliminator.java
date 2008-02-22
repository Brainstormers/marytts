/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package de.dfki.lt.mary.unitselection.adaptation.outlier;

import java.io.IOException;
import java.util.Arrays;

import de.dfki.lt.machinelearning.KMeansClusteringTester;
import de.dfki.lt.machinelearning.KMeansClusteringTrainer;
import de.dfki.lt.mary.unitselection.adaptation.OutlierStatus;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebook;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookFeatureExtractor;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookFile;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookFileHeader;
import de.dfki.lt.signalproc.util.DistanceComputer;
import de.dfki.lt.signalproc.util.MathUtils;

/**
 * @author oytun.turk
 *
 */
public class KMeansMappingEliminator {
    //Separate clusterers
    KMeansClusteringTrainer sourceLsfClusterer;
    KMeansClusteringTrainer sourceF0Clusterer;
    KMeansClusteringTrainer sourceEnergyClusterer;
    KMeansClusteringTrainer sourceDurationClusterer;
    KMeansClusteringTrainer targetLsfClusterer;
    KMeansClusteringTrainer targetF0Clusterer;
    KMeansClusteringTrainer targetEnergyClusterer;
    KMeansClusteringTrainer targetDurationClusterer;
    //
    
    //Joint clusterers
    KMeansClusteringTrainer sourceClusterer;
    KMeansClusteringTrainer targetClusterer;
    //
    
    public void eliminate(KMeansMappingEliminatorParams params,
                          String codebookFileIn, 
                          String codebookFileOut)
    {    
        sourceLsfClusterer = null;
        sourceF0Clusterer = null;
        sourceEnergyClusterer = null;
        sourceDurationClusterer = null;
        targetLsfClusterer = null;
        targetF0Clusterer = null;
        targetEnergyClusterer = null;
        targetDurationClusterer = null;
        sourceClusterer = null;
        targetClusterer = null;
        
        WeightedCodebookFile fileIn = new WeightedCodebookFile(codebookFileIn, WeightedCodebookFile.OPEN_FOR_READ);

        WeightedCodebook codebookIn = null;

        try {
            codebookIn = fileIn.readCodebookFile();
        } catch (IOException e) {
//          TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        int[] acceptanceStatus = new int[codebookIn.header.totalLsfEntries];
        Arrays.fill(acceptanceStatus, OutlierStatus.NON_OUTLIER);

        int totalLsfOutliers = 0;
        int totalDurationOutliers = 0;
        int totalF0Outliers = 0;
        int totalEnergyOutliers = 0;
        int totalOutliers = 0;
        
        if (codebookIn!=null)
        {
            int i;
            int desiredFeatures = 0;

            if (params.isSeparateClustering)
            {
                if (params.isCheckLsfOutliers)
                {
                    sourceLsfClusterer = clusterFeatures(codebookIn, WeightedCodebookFeatureExtractor.LSF_FEATURES, WeightedCodebook.SOURCE, params);
                    targetLsfClusterer = clusterFeatures(codebookIn, WeightedCodebookFeatureExtractor.LSF_FEATURES, WeightedCodebook.TARGET, params);
                }

                if (params.isCheckF0Outliers)
                {
                    sourceF0Clusterer = clusterFeatures(codebookIn, WeightedCodebookFeatureExtractor.F0_FEATURES, WeightedCodebook.SOURCE, params);
                    targetF0Clusterer = clusterFeatures(codebookIn, WeightedCodebookFeatureExtractor.F0_FEATURES, WeightedCodebook.TARGET, params);
                }

                if (params.isCheckEnergyOutliers)
                {
                    sourceEnergyClusterer = clusterFeatures(codebookIn, WeightedCodebookFeatureExtractor.ENERGY_FEATURES, WeightedCodebook.SOURCE, params);
                    targetEnergyClusterer = clusterFeatures(codebookIn, WeightedCodebookFeatureExtractor.ENERGY_FEATURES, WeightedCodebook.TARGET, params);
                }

                if (params.isCheckDurationOutliers)
                {
                    sourceDurationClusterer = clusterFeatures(codebookIn, WeightedCodebookFeatureExtractor.DURATION_FEATURES, WeightedCodebook.SOURCE, params);
                    targetDurationClusterer = clusterFeatures(codebookIn, WeightedCodebookFeatureExtractor.DURATION_FEATURES, WeightedCodebook.TARGET, params);
                }
                
                totalLsfOutliers = eliminate(sourceLsfClusterer, targetLsfClusterer, params.eliminationAlgorithm, acceptanceStatus, OutlierStatus.LSF_OUTLIER, params.eliminationLikelihood);
                totalF0Outliers = eliminate(sourceF0Clusterer, targetF0Clusterer, params.eliminationAlgorithm, acceptanceStatus, OutlierStatus.F0_OUTLIER, params.eliminationLikelihood);
                totalEnergyOutliers = eliminate(sourceEnergyClusterer, targetEnergyClusterer, params.eliminationAlgorithm, acceptanceStatus, OutlierStatus.ENERGY_OUTLIER, params.eliminationLikelihood);
                totalDurationOutliers = eliminate(sourceDurationClusterer, targetDurationClusterer, params.eliminationAlgorithm, acceptanceStatus, OutlierStatus.DURATION_OUTLIER, params.eliminationLikelihood);
            }
            else
            {
                if (params.isCheckLsfOutliers)
                    desiredFeatures += WeightedCodebookFeatureExtractor.LSF_FEATURES;
                if (params.isCheckF0Outliers)
                    desiredFeatures += WeightedCodebookFeatureExtractor.F0_FEATURES;
                if (params.isCheckEnergyOutliers)
                    desiredFeatures += WeightedCodebookFeatureExtractor.ENERGY_FEATURES;
                if (params.isCheckDurationOutliers)
                    desiredFeatures += WeightedCodebookFeatureExtractor.DURATION_FEATURES;
                
                sourceClusterer = clusterFeatures(codebookIn, desiredFeatures, WeightedCodebook.SOURCE, params);
                targetClusterer = clusterFeatures(codebookIn, desiredFeatures, WeightedCodebook.TARGET, params);
                
                totalOutliers = eliminate(sourceClusterer, targetClusterer, params.eliminationAlgorithm, acceptanceStatus, OutlierStatus.GENERAL_OUTLIER, params.eliminationLikelihood);
            }
            
            int newTotalEntries = 0;
            for (i=0; i<codebookIn.header.totalLsfEntries; i++)
            {
                if (acceptanceStatus[i]==OutlierStatus.NON_OUTLIER)
                    newTotalEntries++;
            }
            
            //Write the output codebook
            WeightedCodebookFile codebookOut = new WeightedCodebookFile(codebookFileOut, WeightedCodebookFile.OPEN_FOR_WRITE);
            
            WeightedCodebookFileHeader headerOut = new WeightedCodebookFileHeader(codebookIn.header);
            headerOut.resetTotalEntries();
            codebookOut.writeCodebookHeader(headerOut);

            for (i=0; i<codebookIn.header.totalLsfEntries; i++)
            {
                if (acceptanceStatus[i]==OutlierStatus.NON_OUTLIER)
                    codebookOut.writeLsfEntry(codebookIn.lsfEntries[i]);
            }
            
            //Append pitch statistics
            for (i=0; i<codebookIn.header.totalF0StatisticsEntries; i++)
                codebookOut.writeF0StatisticsEntry(codebookIn.f0StatisticsCollection.entries[i]);
            //
            
            codebookOut.close();
            //
            
            System.out.println("Outliers detected = " + String.valueOf(codebookIn.header.totalLsfEntries-newTotalEntries) + " of " + String.valueOf(codebookIn.header.totalLsfEntries));
            if (params.isSeparateClustering)
            {
                System.out.println("Total lsf outliers = " + String.valueOf(totalLsfOutliers));
                System.out.println("Total f0 outliers = " + String.valueOf(totalF0Outliers));
                System.out.println("Total duration outliers = " + String.valueOf(totalDurationOutliers));
                System.out.println("Total energy outliers = " + String.valueOf(totalEnergyOutliers));
            }
            else
                System.out.println("Total outliers = " + String.valueOf(totalOutliers));
        }
    }
    
    //Collect desired features from codebook and call k-means clustering
    private KMeansClusteringTrainer clusterFeatures(WeightedCodebook codebook, int desiredFeatures, int speakerType, KMeansMappingEliminatorParams params)
    {
        KMeansClusteringTrainer clusterer = null;
        
        double[][] features = codebook.getFeatures(speakerType, desiredFeatures);

        clusterer= new KMeansClusteringTrainer();
        double[] globalVariances = MathUtils.getVarianceCols(features);
        clusterer.cluster(features, params.numClusters, params.maximumIterations, params.minClusterPercent, params.isDiagonalCovariance, globalVariances);
        features = null; //Memory clean-up
        
        return clusterer;
    }
    
    //acceptance status should be initialized properly before calling this function,
    // i.e. it should have the same size as the number of lsf entries in the input codebook
    // all entries should be set to desired values (i.e. OutlierStatus.NON_OUTLIER for first call) since
    // elimination reasons are kept in these entries by summing up in this function
    // eliminationLikelihood should be between 0.0 and 1.0
    private int eliminate(KMeansClusteringTrainer srcClusterer, 
                          KMeansClusteringTrainer tgtClusterer, 
                          int eliminationAlgorithm, 
                          int[] acceptanceStatus, 
                          int desiredOutlierStatus,
                          double eliminationLikelihood)
    {
        int totalOutliers = 0;
        int i, j, k;

        if (eliminationAlgorithm==KMeansMappingEliminatorParams.ELIMINATE_LEAST_LIKELY_MAPPINGS)
        {
            //Find total target clusters for each source cluster and eliminate non-frequent target clusters
            double[][] targetClusterCounts = new double[srcClusterer.clusters.length][]; //Each row correspond to another source cluster
            for (i=0; i<srcClusterer.clusters.length; i++)
            {
                targetClusterCounts[i] = new double[tgtClusterer.clusters.length];
                Arrays.fill(targetClusterCounts[i], 0.0);
            }
            
            for (i=0; i<srcClusterer.clusterIndices.length; i++)
                targetClusterCounts[srcClusterer.clusterIndices[i]][tgtClusterer.clusterIndices[i]] += 1.0;
            
            int[] sortedCountIndices = null;
            double threshold;
            double tempSum;
            int index;
            for (i=0; i<srcClusterer.clusters.length; i++)
            {
                sortedCountIndices = MathUtils.quickSort(targetClusterCounts[i]);
                threshold = eliminationLikelihood*MathUtils.sum(targetClusterCounts[i]);
                tempSum = 0.0;
                index = -1;
                for (j=0; j<targetClusterCounts[i].length; j++)
                {
                    if (tempSum>=threshold)
                        break;
                    
                    tempSum += targetClusterCounts[i][j];
                    index++;
                }
                
                if (index>-1)
                {
                    for (j=0; j<=index; j++)
                    {
                        for (k=0; k<tgtClusterer.clusterIndices.length; k++)
                        {
                            if (srcClusterer.clusterIndices[k]==i && tgtClusterer.clusterIndices[k]==sortedCountIndices[j])
                            {
                                acceptanceStatus[k] += desiredOutlierStatus;
                                totalOutliers++;
                            }
                        }
                    }
                }
            }
        }
        else if (eliminationAlgorithm==KMeansMappingEliminatorParams.ELIMINATE_MEAN_DISTANCE_MISMATCHES)
        {
            
        }
        else if (eliminationAlgorithm==KMeansMappingEliminatorParams.ELIMINATE_USING_SUBCLUSTER_MEAN_DISTANCES)
        {
            
        }
        
        return totalOutliers;
    }

}
