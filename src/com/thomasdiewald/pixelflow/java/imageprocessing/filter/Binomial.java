/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */




package com.thomasdiewald.pixelflow.java.imageprocessing.filter;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;

import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;

public class Binomial {
  
  public DwPixelFlow context;
  
  public static enum BinomialSize{
      KERNEL_3x3    (1, "BINOMIAL_3x3"  )
    , KERNEL_5x5    (2, "BINOMIAL_5x5"  )
    , KERNEL_7x7    (3, "BINOMIAL_7x7"  )
    , KERNEL_9x9    (4, "BINOMIAL_9x9"  )
    , KERNEL_11x11  (5, "BINOMIAL_11x11")
    , KERNEL_13x13  (6, "BINOMIAL_13x13")
    , KERNEL_15x15  (7, "BINOMIAL_15x15")
    ;
    
    protected DwGLSLProgram shader_horz, shader_vert;
    protected int radius;
    protected String define;
    
    private BinomialSize(int radius, String define){
      this.radius = radius;
      this.define = define;
    }
    
    private void buildShader(DwPixelFlow context){
      if(shader_horz != null && shader_vert != null){
        return; // no need to rebuild
      }
      
      boolean push_state = DwGLSLProgram.BUILD_IN_CONSTRUCTOR;
      
      DwGLSLProgram.BUILD_IN_CONSTRUCTOR = false;

      Object id_horz = this.name() + "horz";
      Object id_vert = this.name() + "vert";
      
      shader_horz = context.createShader(id_horz, DwPixelFlow.SHADER_DIR+"Filter/binomial.frag");
      shader_vert = context.createShader(id_vert, DwPixelFlow.SHADER_DIR+"Filter/binomial.frag");
      
      shader_horz.frag.getDefine("HORZ").value = "1";
      shader_horz.frag.getDefine(define).value = "1";
      
      shader_vert.frag.getDefine("VERT").value = "1";
      shader_vert.frag.getDefine(define).value = "1";
      
      shader_horz.build();
      shader_vert.build();
      
      DwGLSLProgram.BUILD_IN_CONSTRUCTOR = push_state;
    }
    
  }
  
  public void printCoeffs(){
    int r = 7;
    int N = r*2+1;
//    int binomial_norm = 1 << (N - 1);

    int[] B = new int[N];

    B[0] = 1;
    System.out.printf("[%2d] %8d | ",0,  1 << ((0+1) - 1));
    System.out.printf("%6d\n", B[0]);

    for (int i = 1; i < N; i++){

      int prev = B[0];
      System.out.printf("[%2d] %8d | ", i,  1 << ((i+1) - 1));
      System.out.printf("%6d", B[0]);
      for (int j = 1; j < i; j++){
        int curr = B[j];
        B[j] = prev + curr;
        System.out.printf("%6d", B[j]);

        prev = curr;
      }
      B[i] = 1;
      System.out.printf("%6d\n", B[i]);
    }

  }

  public Binomial(DwPixelFlow context){
    this.context = context;
  }



  public void apply(PGraphicsOpenGL src, PGraphicsOpenGL dst, PGraphicsOpenGL tmp, BinomialSize kernel) {
    if(src == tmp || dst == tmp){
      System.out.println("BoxBlur error: read-write race");
      return;
    }
    if(kernel == null){
      return; 
    }

    Texture tex_src = src.getTexture(); if(!tex_src.available())  return;
    Texture tex_dst = dst.getTexture(); if(!tex_dst.available())  return;
    Texture tex_tmp = tmp.getTexture(); if(!tex_tmp.available())  return;
    
    kernel.buildShader(context);
    
//    tmp.beginDraw();
    context.begin();
    context.beginDraw(tmp);
    pass(tex_src.glName, tmp.width, tmp.height, kernel.shader_horz);
    context.endDraw();
    context.end("Binomial.apply - HORZ");
//    tmp.endDraw();

//    dst.beginDraw();
    context.begin();
    context.beginDraw(dst);
    pass(tex_tmp.glName, dst.width, dst.height, kernel.shader_vert);
    context.endDraw();
    context.end("Binomial.apply - VERT");
//    dst.endDraw(); 
  }
  
  

  private void pass(int tex_handle, int w, int h, DwGLSLProgram shader){
    shader.begin();
    shader.uniform2f("wh_rcp", 1f/w,  1f/h);
    shader.uniformTexture("tex", tex_handle);
    shader.drawFullScreenQuad();
    shader.end();
  }
  
}