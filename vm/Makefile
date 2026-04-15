# Compiler and Flags
CC = cl

# Combine flags! 
# Added /Zi for debug symbols if it crashes
CFLAGS = /nologo /W3 /O2 /Ot /GL /arch:AVX2 /Iinclude /Zi /Fd$(OBJDIR)\

# Directories
SRCDIR = src
OBJDIR = obj

# The final executable name
TARGET = main.exe

# Object files
OBJS = $(OBJDIR)\main.obj $(OBJDIR)\memory.obj $(OBJDIR)\cpu.obj $(OBJDIR)\os.obj

# Linker Flags:
LDFLAGS = /link /INCREMENTAL:NO /LTCG /PDB:$(OBJDIR)\main.pdb

all: directories $(TARGET)

directories:
    @if not exist $(OBJDIR) mkdir $(OBJDIR)

$(TARGET): $(OBJS)
    $(CC) $(CFLAGS) $(OBJS) /Fe$(TARGET) $(LDFLAGS)

{$(SRCDIR)}.c{$(OBJDIR)}.obj:
    $(CC) $(CFLAGS) /c $< /Fo$@

clean:
    @if exist $(OBJDIR) rd /s /q $(OBJDIR)
    @if exist $(TARGET) del $(TARGET)
    @if exist *.pdb del *.pdb
    @if exist *.ilk del *.ilk
    @if exist *.obj del *.obj