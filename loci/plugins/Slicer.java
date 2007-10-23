//
// Slicer.java
//

/*
LOCI Plugins for ImageJ: a collection of ImageJ plugins including the
4D Data Browser, Bio-Formats Importer, Bio-Formats Exporter and OME plugins.
Copyright (C) 2006-@year@ Melissa Linkert, Christopher Peterson,
Curtis Rueden, Philip Huettl and Francis Wong.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU Library General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Library General Public License for more details.

You should have received a copy of the GNU Library General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package loci.plugins;

import ij.*;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import loci.formats.FormatTools;

public class Slicer implements PlugIn {

  // -- Fields --

  /** Flag indicating whether last operation was canceled. */
  public boolean canceled;

  // -- PlugIn API methods --

  /** Executes the plugin. */
  public void run(String arg) {
    boolean sliceZ = false;
    boolean sliceC = false;
    boolean sliceT = false;
    String stackOrder = null;
    boolean keepOriginal = false;

    if (arg == null || arg.trim().equals("")) {
      // prompt for slicing options

      GenericDialog gd = new GenericDialog("Slicing options...");
      gd.addCheckbox("Split channels", false);
      gd.addCheckbox("Split Z slices", false);
      gd.addCheckbox("Split timepoints", false);
      gd.addCheckbox("Keep original stack", false);
      gd.addChoice("Stack order", new String[] {"XYCZT", "XYCTZ", "XYZCT",
        "XYZTC", "XYTCZ", "XYTZC"}, "XYCZT");
      gd.showDialog();

      if (gd.wasCanceled()) {
        canceled = true;
        return;
      }

      sliceC = gd.getNextBoolean();
      sliceZ = gd.getNextBoolean();
      sliceT = gd.getNextBoolean();
      keepOriginal = gd.getNextBoolean();
      stackOrder = gd.getNextChoice();
    }
    else {
      sliceZ = Boolean.parseBoolean(Macro.getValue(arg, "slice_z", "false"));
      sliceC = Boolean.parseBoolean(Macro.getValue(arg, "slice_c", "false"));
      sliceT = Boolean.parseBoolean(Macro.getValue(arg, "slice_t", "false"));
      keepOriginal = Boolean.parseBoolean(Macro.getValue(arg,
        "keep_original", "false"));
      stackOrder = Macro.getValue(arg, "stack_order", "XYCZT");
    }

    ImagePlus current = WindowManager.getCurrentImage();
    ImageStack stack = current.getImageStack();

    if (current instanceof CompositeImage) return;

    int sizeZ = current.getNSlices();
    int sizeC = current.getNChannels();
    int sizeT = current.getNFrames();

    if (current instanceof CustomImage) sliceC = false;

    int slicesPerStack = stack.getSize();
    if (sliceZ) slicesPerStack /= sizeZ;
    if (sliceC) slicesPerStack /= sizeC;
    if (sliceT) slicesPerStack /= sizeT;

    ImageStack[] newStacks =
      new ImageStack[stack.getSize() / slicesPerStack];
    for (int i=0; i<newStacks.length; i++) {
      newStacks[i] = new ImageStack(stack.getWidth(), stack.getHeight());
    }

    for (int i=0; i<sizeZ*sizeC*sizeT; i++) {
      int[] zct = FormatTools.getZCTCoords(stackOrder, sizeZ, sizeC, sizeT,
        stack.getSize(), i);
      int stackNdx = FormatTools.getIndex(stackOrder, sliceZ ? sizeZ : 1,
        sliceC ? sizeC : 1, sliceT ? sizeT : 1, newStacks.length,
        sliceZ ? zct[0] : 0, sliceC ? zct[1] : 0, sliceT ? zct[2] : 0);

      newStacks[stackNdx].addSlice(stack.getSliceLabel(i + 1),
        stack.getProcessor(i + 1));
    }

    for (int i=0; i<newStacks.length; i++) {
      ImagePlus ip = new ImagePlus(current.getTitle(), newStacks[i]);
      ip.setDimensions(sliceC ? 1 : sizeC, sliceZ ? 1 : sizeZ,
        sliceT ? 1 : sizeT);
      if (current instanceof CustomImage) {
        CustomImage c = new CustomImage(ip, stackOrder, sliceZ ? 1 : sizeZ,
          sliceT ? 1 : sizeT, sliceC ? 1 : sizeC, true);
        c.show();
      }
      else ip.show();
    }
    if (!keepOriginal) current.close();
  }

}
