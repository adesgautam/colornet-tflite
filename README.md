# Colornet-tflite
### This is an implementation of running a image colorization model on Android.

The motive of this project is to build a pipeline to train and deploy models on Android using Tflite and not to create a good model for colorization.

More training and better dataset can improve the results.

### Models
* Pretrained MobileNet
* Custom Encoder-Decoder

The input image is passed through MobileNet and the output from the last layer is concatenated with the Encoder output and then fed to the Decoder. The output of the Decoder is a colorized image.

The model is trained on just 30k images in the Flikr30k dataset. The results are not so good. Brownish images are generated by the model.

### Model Architecture

```
__________________________________________________________________________________________________
Layer (type)                    Output Shape         Param #     Connected to                     
==================================================================================================
input_3 (InputLayer)            [(None, 256, 256, 1) 0                                            
__________________________________________________________________________________________________
conv2d (Conv2D)                 (None, 128, 128, 64) 640         input_3[0][0]                    
__________________________________________________________________________________________________
conv2d_1 (Conv2D)               (None, 128, 128, 128 73856       conv2d[0][0]                     
__________________________________________________________________________________________________
conv2d_2 (Conv2D)               (None, 64, 64, 128)  147584      conv2d_1[0][0]                   
__________________________________________________________________________________________________
conv2d_3 (Conv2D)               (None, 64, 64, 256)  295168      conv2d_2[0][0]                   
__________________________________________________________________________________________________
conv2d_4 (Conv2D)               (None, 32, 32, 256)  590080      conv2d_3[0][0]                   
__________________________________________________________________________________________________
conv2d_5 (Conv2D)               (None, 32, 32, 512)  1180160     conv2d_4[0][0]                   
__________________________________________________________________________________________________
input_2 (InputLayer)            [(None, 1000)]       0                                            
__________________________________________________________________________________________________
conv2d_6 (Conv2D)               (None, 32, 32, 512)  2359808     conv2d_5[0][0]                   
__________________________________________________________________________________________________
repeat_vector (RepeatVector)    (None, 1024, 1000)   0           input_2[0][0]                    
__________________________________________________________________________________________________
conv2d_7 (Conv2D)               (None, 32, 32, 256)  1179904     conv2d_6[0][0]                   
__________________________________________________________________________________________________
reshape (Reshape)               (None, 32, 32, 1000) 0           repeat_vector[0][0]              
__________________________________________________________________________________________________
concatenate (Concatenate)       (None, 32, 32, 1256) 0           conv2d_7[0][0]                   
                                                                 reshape[0][0]                    
__________________________________________________________________________________________________
conv2d_8 (Conv2D)               (None, 32, 32, 256)  321792      concatenate[0][0]                
__________________________________________________________________________________________________
conv2d_9 (Conv2D)               (None, 32, 32, 128)  295040      conv2d_8[0][0]                   
__________________________________________________________________________________________________
conv2d_transpose (Conv2DTranspo (None, 64, 64, 128)  147584      conv2d_9[0][0]                   
__________________________________________________________________________________________________
conv2d_10 (Conv2D)              (None, 64, 64, 64)   73792       conv2d_transpose[0][0]           
__________________________________________________________________________________________________
conv2d_transpose_1 (Conv2DTrans (None, 128, 128, 64) 36928       conv2d_10[0][0]                  
__________________________________________________________________________________________________
conv2d_11 (Conv2D)              (None, 128, 128, 32) 18464       conv2d_transpose_1[0][0]         
__________________________________________________________________________________________________
conv2d_12 (Conv2D)              (None, 128, 128, 16) 4624        conv2d_11[0][0]                  
__________________________________________________________________________________________________
conv2d_13 (Conv2D)              (None, 128, 128, 2)  290         conv2d_12[0][0]                  
__________________________________________________________________________________________________
conv2d_transpose_2 (Conv2DTrans (None, 256, 256, 2)  38          conv2d_13[0][0]                  
==================================================================================================
Total params: 6,725,752
Trainable params: 6,725,752
Non-trainable params: 0
```

![model](https://github.com/adesgautam/colornet-tflite/blob/master/images/model.png?raw=true "Model Architecture")
