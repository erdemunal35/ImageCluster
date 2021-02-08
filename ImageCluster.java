import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

// my imports
import java.lang.Math;
import java.awt.Color;
import java.util.Arrays;
import java.util.ArrayList;

public class ImageCluster {

	Map<String, ColorLayoutDescriptor> index = new HashMap<String, ColorLayoutDescriptor>();
	Map<String, Integer> cluster_index = new HashMap<String, Integer>();

	/*
	 * do something here...
	 * k = number of clusters
	 */
	public void createClusters(String outputPath, int k) {

		// todo: implement clustering - create subfolders ...

		if (k == 1) {
			File outputFolder = new File(outputPath + "\\clusters\\");

			if (!outputFolder.exists())
				outputFolder.mkdir();

			for (String file : index.keySet()) {

				copyFile(file, outputFolder.getPath() + "\\" + new File(file).getName());
			}
		}
		else if(k== 0) {
			System.out.println("Cannot create 0 number of clusters.");
		}
		//Assuming user will not enter more other invalid characters
		else {
			clusterFiles(k);
			// for(String file: cluster_index.keySet()){
			// 	System.out.println(cluster_index.get(file));
			// }
			File outputFolder = new File(outputPath + "\\clusters\\");

			if (!outputFolder.exists())
				outputFolder.mkdir();

			for(int i = 0; i<k; i++){
				outputFolder = new File(outputPath + "\\clusters\\"+"cluster "+i+"\\");

				if (!outputFolder.exists())
					outputFolder.mkdir();
			}

			outputFolder = new File(outputPath + "\\clusters\\");
			for(String file: cluster_index.keySet()){
				copyFile(file, outputFolder.getPath() + "\\cluster " + cluster_index.get(file) + "\\" + new File(file).getName());
			}

		}
	}

	/*
	 * do something here...
	 */
	public ColorLayoutDescriptor createCLD(BufferedImage img) {
		
		int r = img.getHeight();
		int c = img.getWidth();
		int r8 = (int)Math.floor((double)r/8);
		int c8 = (int)Math.floor((double)c/8);

		int temp_red = 0;
		int temp_green = 0;
		int temp_blue = 0;
		double y_blocks [][] = new double[8][8];
		double cb_blocks [][] = new double[8][8];
		double cr_blocks [][] = new double[8][8];

		for(int temp_block_y = 0; temp_block_y<8; temp_block_y++) {
			for(int temp_block_x = 0; temp_block_x<8; temp_block_x++){
				for(int row_index = 0 +temp_block_y*r8; row_index < r8+temp_block_y*r8; row_index++){
					for(int col_index = 0 + temp_block_x*c8; col_index < c8+temp_block_x*c8; col_index++){
						int pixel = img.getRGB(col_index,row_index);
			            //Creating a Color object from pixel value
			            Color color = new Color(pixel, true);
			            //Retrieving the R G B values
			            int red = color.getRed();
			            int green = color.getGreen();
			            int blue = color.getBlue();
						temp_red += red;
						temp_green += green;
						temp_blue += blue;
					}
				}
				//Get the mean rgb values for both 8x8 matrixes
				temp_red = temp_red/(r8*c8);
				temp_green = temp_green/(r8*c8);
				temp_blue = temp_blue/(r8*c8);

				y_blocks[temp_block_y][temp_block_x] = 0.299*temp_red + 0.587*temp_green + 0.114*temp_blue;
				cb_blocks[temp_block_y][temp_block_x] = 128 - (0.168736 * temp_red) - (0.331264 * temp_green) + 0.5*temp_blue;
				cr_blocks[temp_block_y][temp_block_x] = 128 + 0.5*temp_red - (0.418688*temp_green) - (0.081312*temp_blue);

				temp_red = 0;
				temp_green = 0;
				temp_blue = 0;
			}
		}
		

		double yDCT[][] = getDCT(y_blocks, 8);
		double cbDCT[][] = getDCT(cb_blocks, 8);
		double crDCT[][] = getDCT(cr_blocks, 8);

		// Debug
		// for(int temp_block_y = 0; temp_block_y<8; temp_block_y++) {
		// 	for(int temp_block_x = 0; temp_block_x<8; temp_block_x++){
		// 		System.out.print(crDCT[temp_block_y][temp_block_x] + ", ");

		// 	}
		// 	System.out.println();
		// }

		double ac_yDCT[] = new double[] {yDCT[0][1], yDCT[1][0], yDCT[2][0], yDCT[1][1], yDCT[0][2] };
		double ac_cbDCT[] = new double[] { cbDCT[0][1], cbDCT[1][0] };
		double ac_crDCT[] = new double[] { crDCT[0][1], crDCT[1][0] };
		ColorLayoutDescriptor cld = new ColorLayoutDescriptor(yDCT[0][0], cbDCT[0][0], crDCT[0][0], ac_yDCT, ac_cbDCT, ac_crDCT);
		
		return cld;
	}

	//Gets DCT conversion for the given block (length x length)
	public double [][] getDCT(double block[][], int length) {
		double result [][] = new double[length][length];
	
		for(int temp_block_y = 0; temp_block_y<length; temp_block_y++) {
			for(int temp_block_x = 0; temp_block_x<length; temp_block_x++){

				double alpha_u, alpha_v;
				if(temp_block_y == 0)
					alpha_u = Math.sqrt(1/(double)length);
				else
					alpha_u = Math.sqrt(2/(double)length);

				if(temp_block_x == 0)
					alpha_v = Math.sqrt(1/(double)length);
				else
					alpha_v = Math.sqrt(2/(double)length);

				double sum = 0;
				for(int y = 0; y<length; y++){
					for(int x = 0; x<length; x++){
						sum += block[y][x] * Math.cos(Math.PI *(2*y+1)*temp_block_y/(2*length)) * Math.cos(Math.PI *(2*x+1)*temp_block_x/(2*length));
					}
				}
				result[temp_block_y][temp_block_x] = alpha_u*alpha_v*sum;
			}
		}	
		
		// System.out.println("Exiting getDCT");
		return result;
		
	}
	/*
	 * for convenience
	 */
	public void copyFile(String from, String to) {
		InputStream is = null;
		OutputStream os = null;
		try {
			try {
				is = new FileInputStream(new File(from));
				os = new FileOutputStream(new File(to));
				byte[] buffer = new byte[1024];
				int length;
				while ((length = is.read(buffer)) > 0) {
					os.write(buffer, 0, length);
				}

			} finally {
				is.close();
				os.close();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * for convenience
	 */
	public void indexFile(File file) throws IOException {

		if (!index.containsKey(file.getPath())) {
			BufferedImage img = null;
			try {
				img = ImageIO.read(file);
				index.put(file.getPath(), this.createCLD(img));

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Run this method after indexing all the images to "index" HashMap
	// This method simply fills the other HashMap called "cluster_index", k: number of clusters (>1)
	// This method follows "Hierarchical clustering", for more info: https://en.wikipedia.org/wiki/Hierarchical_clustering
	// Clustering stops until there are total amount of 'k' clusters
	public void clusterFiles(int k) {

		//Assign all images to different clusters first
		int cluster_counter = 0;
		for (String file : index.keySet()) {
			cluster_index.put(file, cluster_counter++);
		}
		// For debug
		// printClusterIndex();

		String files[] = new String[cluster_index.size()];
		int i = 0;
		for(String file: cluster_index.keySet())
			files[i++] = file;


		boolean notTheFirstTimeInWhile = false;
		while(cluster_counter > k) {
			// System.out.println("While: " + cluster_counter);
			for(String file: index.keySet()) {
				double distance = Double.MAX_VALUE;
				double temp_distance = Double.MAX_VALUE;
				String temp_file2 = file;
				for(String file2: index.keySet()){
					if(cluster_index.get(file) != cluster_index.get(file2)){
						// notTheFirstTimeInWhile simply prevents the algorithm to overfit
						// Otherwise first clusters are filled with the most images and last clusters ,eg: 'k-1' cluster, will have only 1 image in most cases
						// In other words, it provides consistent, stable distribution of images into clusters
						if(notTheFirstTimeInWhile){
							if((cluster_index.get(file)>=k || cluster_index.get(file2)>=k)){
								temp_distance = index.get(file).calculateSimilarity(index.get(file2));
								if(temp_distance<distance){
									distance = temp_distance;
									temp_file2 = file2;
								}
							}
						}
						else{
							temp_distance = index.get(file).calculateSimilarity(index.get(file2));
							if(temp_distance<distance){
								distance = temp_distance;
								temp_file2 = file2;
							}
						}
						
					}
				}

				// Cluster numbers of file and file2 are merged
				// The YDC, YAC, CbDC, CbAC ,CrDC and CrAC coefficents of all images in the chosen cluster are uptaded,
				// In other words, mean of the cluster group is slightly updated by the new added image
				if(!temp_file2.equals(file) && cluster_counter!= k) {
					// System.out.println("Inside if !temp_file2.....");
					// System.out.println(file + " " + cluster_index.get(file));
					// System.out.println(temp_file2 + " " + cluster_index.get(temp_file2));
					if(cluster_index.get(file)<cluster_index.get(temp_file2))
						cluster_index.replace(temp_file2, cluster_index.get(file));
					else
						cluster_index.replace(file, cluster_index.get(temp_file2));
					int curr_cluster = cluster_index.get(file);
					double temp1 = 0.0;
					double temp2 = 0.0;
					double temp3 = 0.0;
					double temp4[] = new double[]{0.0, 0.0, 0.0, 0.0, 0.0};
					double temp5[] = new double[]{0.0, 0.0};
					double temp6[] = new double[]{0.0, 0.0};
					int temp_counter = 0;
					for(String file3 : cluster_index.keySet()){
						if(cluster_index.get(file3) == curr_cluster){
							temp_counter++;
							temp1 += index.get(file3).getYDCCoeff();
							temp2 += index.get(file3).getCbDCCoeff();
							temp3 += index.get(file3).getCrDCCoeff();
							for(i = 0; i<5; i++)
								temp4[i] += index.get(file3).getYACCoeff()[i];
							for(i = 0; i<2; i++){
								temp5[i] += index.get(file3).getCbACCoeff()[i];
								temp6[i] += index.get(file3).getCrACCoeff()[i];
							}
						}
					}
					temp1 = temp1/(double)temp_counter;
					temp2 = temp2/(double)temp_counter;
					temp3 = temp3/(double)temp_counter;
					for(i = 0; i<5; i++)
						temp4[i] = temp4[i]/(double)temp_counter;
					for(i = 0; i<2; i++){
						temp5[i] = temp5[i]/(double)temp_counter;
						temp6[i] = temp6[i]/(double)temp_counter;
					}
					for(String file3 : cluster_index.keySet()){
						if(cluster_index.get(file3) == curr_cluster){
							index.get(file3).setYDCCoeff(temp1);
							index.get(file3).setCbDCCoeff(temp2);
							index.get(file3).setCrDCCoeff(temp3);
							index.get(file3).setYACCoeff(temp4);
							index.get(file3).setCbACCoeff(temp5);
							index.get(file3).setCrACCoeff(temp6);
						}
					}

					cluster_counter = getNumOfClusters();
					// System.out.println("cluster_counter: " + cluster_counter);
					
				}
			}

			notTheFirstTimeInWhile = true;
		}

		// Normalize cluster values
		ArrayList<Integer> temp_list = new ArrayList<Integer>();
		for(String file: cluster_index.keySet()){
			int value = cluster_index.get(file);
			if(!temp_list.contains(value))
				temp_list.add(value);
		}

		for(String file: cluster_index.keySet()){
			int value = cluster_index.get(file);
			int index = temp_list.indexOf(value);
			cluster_index.replace(file, index);
		}

	}

	// Returns the number of different clusters in cluster_index
	public int getNumOfClusters(){
		ArrayList<Integer> temp_list = new ArrayList<Integer>();

		for(String file: cluster_index.keySet()){
			int value = cluster_index.get(file);
			if(!temp_list.contains(value))
				temp_list.add(value);
		}

		return temp_list.size();
	}

	public void printIndex(){
		for(String file: index.keySet()){
			System.out.print(index.get(file).getYDCCoeff()+ " ");
			System.out.print(index.get(file).getCbDCCoeff() + " ");
			System.out.println(index.get(file).getCrDCCoeff());
		}
	}

	public void printClusterIndex(){
		for(String file: cluster_index.keySet()){
			System.out.print(file+ " ");
			System.out.println(cluster_index.get(file));
		}
	}

	public static void main(String[] args) {

		ImageCluster ic = new ImageCluster();

		System.out.println("Enter path to image folder:\n");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			String file = br.readLine();
			System.out.println("Enter the number of clusters (k):\n");
			try{
				int num_clusters = Integer.parseInt(br.readLine());
				File imageFolder = new File(file);
				if (imageFolder.exists() && imageFolder.isDirectory()) {

					int resno = 0;

					for (String filename : imageFolder.list()) {
						if (filename.endsWith(".jpg")) {

							ic.indexFile(new File(imageFolder.getPath() + "\\" + filename));
							System.out.println("indexed " + filename);
							resno++;
							// if(resno == 100)
							// 	break;
						}
					}

					System.out.println("indexed " + resno + " images");
					ic.createClusters(imageFolder.getParent(), num_clusters);
					System.out.println(resno + " images are divided into " + num_clusters + " clusters");
					// ic.printIndex();
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	class ColorLayoutDescriptor {

		private double YDCCoeff, CbDCCoeff, CrDCCoeff;
		private double[] YACCoeff, CbACCoeff, CrACCoeff;
		
		// Default constructor, all variables are assigned to 0		
		public ColorLayoutDescriptor() {

			this.YDCCoeff = 0;
			this.CbDCCoeff = 0;
			this.CrDCCoeff = 0;
			this.YACCoeff = new double[] { 0, 0, 0, 0, 0 };
			this.CbACCoeff = new double[] {0, 0};
			this.CrACCoeff = new double[] {0, 0};

		}
		// Main constructor
		public ColorLayoutDescriptor(double YDCCoeff, double CbDCCoeff, double CrDCCoeff, double[] YACCoeff, double[] CbACCoeff, double[] CrACCoeff) {
			this.YDCCoeff = YDCCoeff;
			this.CbDCCoeff = CbDCCoeff;
			this.CrDCCoeff = CrDCCoeff;
			this.YACCoeff = YACCoeff;
			this.CbACCoeff = CbACCoeff;
			this.CrACCoeff = CrACCoeff;
		}

		
		// The two CLDs get similar as they get closer to 0
		public double calculateSimilarity(ColorLayoutDescriptor cld) {
			// compare this with given cld
			// These weights work fine, DC weights might be increased althought no benefit is observed
			int w = 2;
			int w1 = 2;
			int w_1 = 2;
			int w2 = 2;

			double result = 0.0;
			result += Math.sqrt(w*Math.pow((this.YDCCoeff - cld.getYDCCoeff()), 2));
			result += Math.sqrt(w1*Math.pow((this.CbDCCoeff - cld.getCbDCCoeff()), 2));
			result += Math.sqrt(w1*Math.pow((this.CrDCCoeff - cld.getCrDCCoeff()), 2));
			for(int i = 0; i<5; i++)
				result += Math.sqrt(w_1*Math.pow((this.YACCoeff[i] - cld.getYACCoeff()[i]), 2));
			for(int i = 0; i<2; i++){
				result += Math.sqrt(w2*Math.pow((this.CbACCoeff[i] - cld.getCbACCoeff()[i]), 2));
				result += Math.sqrt(w2*Math.pow((this.CrACCoeff[i] - cld.getCrACCoeff()[i]), 2));
			}
			return result;
		}

		// Getters
		public double getYDCCoeff(){
			return YDCCoeff;
		}
		public double getCbDCCoeff(){
			return CbDCCoeff;
		}
		public double getCrDCCoeff(){
			return CrDCCoeff;
		}
		public double[] getYACCoeff(){
			return YACCoeff;
		}
		public double[] getCbACCoeff(){
			return CbACCoeff;
		}
		public double[] getCrACCoeff(){
			return CrACCoeff;
		}

		// Setters
		public void setYDCCoeff(double a){
			YDCCoeff = a;
		}
		public void setCbDCCoeff(double a){
			CbDCCoeff = a;
		}
		public void setCrDCCoeff(double a){
			CrDCCoeff = a;
		}
		public void setYACCoeff(double[] a){
			for(int i = 0 ; i<a.length; i++)
				YACCoeff[i] = a[i];
		}
		public void setCbACCoeff(double[] a){
			for(int i = 0 ; i<a.length; i++)
				CbACCoeff[i] = a[i];
		}
		public void setCrACCoeff(double[] a){
			for(int i = 0 ; i<a.length; i++)
				CrACCoeff[i] = a[i];
		}

	}


}