# Embedded Graph Drawing
Small extension to Java Jung to work on embedded graphs.
This contains an implementation for drawing graphs NIC-planar 1-bend RAC in quadratic area.
This implementation is based on the article "Compact Drawings of 1-Planar Graphs with Right-Angle Crossings and Few Bends", which can be found at https://arxiv.org/abs/1806.10044 – see also https://doi.org/10.1016/j.comgeo.2019.07.006 for the journal version.

---------------------------------------

### Setup and Run the Program

Navigate to the top-level directory of this project (where the pom.xml is).
Beside a sufficiently new Java version you need maven to be installed.
Build via maven:

    mvn package

Execute the sample main method:

    mvn exec:java -Dexec.mainClass="de.uniwue.informatik.main.DrawGraphs"

This will open a window showing some stages of the algorithm for creating a NIC-planar drawing with right-angle crossings on a quadratic size grid of a sample NIC-planar graph.
The stages are as follows (the next stage appears by pressing the "Step" button).
1. The drawing is the one produced by the shift algorithm of Harel and Sardas, where the crossing edges have been removed and there is a divided kite for each crossing (containing dummy edges).
2. The drawing of the previous stage scaled up by a facator of 2.
3. The drawing after inserting the 1-bend edges that cross in a right angle inside the kites. There are three cases with one case appearing per kite (for a description see the article).
4. The drawing after removing the dummy edges. This is the final NIC-planar 1-bend RAC drawing of the input sample graph.

For the displayed graph drawings, there is also an output as ipe files at:

    target/drawings/

The source code is available.
You may try different graphs and things.

---------------------------------------

### Contact
For any question regarding this project send an email to: [zink@informatik.uni-wuerzburg.de](mailto:zink@informatik.uni-wuerzburg.de)
