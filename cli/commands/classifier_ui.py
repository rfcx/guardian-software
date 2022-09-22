import tkinter as tk
from commands.common.classifier_command import *
import json

class RemoveClassifierPage(tk.Frame):

   def __init__(self, parent, controller):
      tk.Frame.__init__(self, parent)
      self.device = controller.getDevice()

      self.columnconfigure([0, 1, 2, 3, 4, 5], weight = 1)
      label = tk.Label(self, text = "Classifiers", font = ("Courier", 14), anchor = "center")
      label.grid(row = 0, column = 0, sticky = "nsew", columnspan = 5)

      self.classifierLabels = {}
      self.chainsaw = tk.Label(self, text = "chainsaw", font = ("Courier", 14), anchor = "center")
      self.chainsaw.grid(row = 1, column = 0, sticky = "nsew", columnspan = 4)
      self.elephant = tk.Label(self, text = "asis-elephant-edge", font = ("Courier", 14), anchor = "center")
      self.elephant.grid(row = 2, column = 0, sticky = "nsew", columnspan = 4)

      self.chainsawButton = tk.Button(self, text = "Install v5", fg = 'white', bg = "blue", width = 20, height = 2, anchor = "center", command = lambda: self.install("chainsaw"))
      self.chainsawButton.grid(row = 1, column = 5, sticky = "nsew", columnspan = 5)
      self.elephantButton = tk.Button(self, text = "Install v2", fg = 'white', bg = "blue", width = 20, height = 2, anchor = "center", command = lambda: self.install("asia-elephant-edge"))
      self.elephantButton.grid(row = 2, column = 5, sticky = "nsew", columnspan = 5)

      removeButton = tk.Button(self, text = "Remove all classifiers", fg = "white", bg = "red", width = 50, height = 2, anchor = "center", command = lambda: self.remove())
      removeButton.grid(row = 3, column = 0, sticky = "nsew", columnspan = 7)

      self.labels = { "chainsaw": self.chainsaw, "asia-elephant-edge": self.elephant }
      self.buttons = { "chainsaw": self.chainsawButton, "asia-elephant-edge": self.elephantButton }
      self.downloadVersions = { "chainsaw": "5", "asia-elephant-edge": "2" }

      self.refresh()

   def refresh(self):
      classifiers = getClassifiers(self.device)
      
      self.chainsaw.config(text = f"Current chainsaw version: {classifiers.get('chainsaw')}")
      self.elephant.config(text = f"Current elephant version: {classifiers.get('asia-elephant-edge')}")
            
      for key in self.buttons:
         if classifiers.get(key) == self.downloadVersions[key]:
            self.buttons[key].config(state = "disabled", text = "up to date")
         else:
            self.buttons[key].config(state = "normal", text = f"Install v{self.downloadVersions[key]}")

   def install(self, classifierName):
      downloadClassifier(self.device, classifierName)
      self.refresh()

   def remove(self):
      removeClassifiers(self.device)
      self.refresh()
